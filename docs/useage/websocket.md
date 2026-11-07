# mica-net WebSocket 使用文档

> mica-net-websocket 基于 mica-net-core 的 TCP + mica-net-http 的握手升级，提供 `WsServerStarter` + `IWsMsgHandler`，快速搭建 WebSocket 服务。

## 1. 引入依赖

```xml
<dependency>
    <groupId>net.dreamlu</groupId>
    <artifactId>mica-net-http</artifactId>
    <version>${mica-net.version}</version>
    <!-- 同一个依赖同时包含 HTTP 与 WebSocket 服务端能力 -->
</dependency>
```

## 2. 核心概念

| 名称 | 作用 |
| ---- | ---- |
| `WsServerStarter` | 一键启动：`port` + `IWsMsgHandler` 即可对外提供 WS 服务 |
| `IWsMsgHandler` | 业务接口：`onText` / `onBytes` / `onClose` / `onPong` / `handshake` / `onAfterHandshaked` |
| `WsRequest` | 携带连接信息：`ChannelContext`、`headers`、`pathParam` 等 |
| `ChannelContext` | TCP 连接上下文，可绑定 `userId` / `group` / `send` / `close` |
| `Opcode` | 区分 TEXT/BINARY/CONTINUATION/CLOSE/PING/PONG 等 |
| `WsCloseCode` | 关闭码常量，如 `NORMAL`(1000)、`PROTOCOL_ERROR`(1002) |
| `Ws` | 操作门面：主动 `close` / `ping` / `pong` |

## 3. 最简单的 Echo Server

```java
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.websocket.common.WsRequest;
import net.dreamlu.mica.net.websocket.server.WsServerStarter;
import net.dreamlu.mica.net.websocket.server.handler.IWsMsgHandler;

public class WsEchoServer {
    public static void main(String[] args) throws Exception {
        IWsMsgHandler handler = new IWsMsgHandler() {
            @Override
            public Object onText(WsRequest request, String text, ChannelContext ctx) {
                System.out.println("recv: " + text);
                return text; // 直接返回字符串 = 回写文本帧
            }
        };
        new WsServerStarter(8080, handler).start();
        System.out.println("ws://localhost:8080");
    }
}
```

```bash
# 浏览器控制台
let ws = new WebSocket("ws://localhost:8080");
ws.onmessage = e => console.log("recv", e.data);
ws.onopen    = () => ws.send("hello");
```

## 4. onText / onBytes / onClose

`IWsMsgHandler` 的所有方法都有 default 实现，按需覆盖即可：

```java
IWsMsgHandler handler = new IWsMsgHandler() {

    /** 收到 TEXT 帧 */
    @Override
    public Object onText(WsRequest request, String text, ChannelContext ctx) {
        return "echo-" + text;
    }

    /** 收到 BINARY 帧 */
    @Override
    public Object onBytes(WsRequest request, byte[] bytes, ChannelContext ctx) {
        // 返回 byte[] / ByteBuffer / null 都支持
        return bytes;
    }

    /** 收到 CLOSE 帧（一般不用处理） */
    @Override
    public void onClose(WsRequest request, byte[] bytes, ChannelContext ctx) {
        System.out.println("client closed");
    }

    /** 收到 PONG 帧（框架收到 PING 会自动回 PONG，这里是对端主动发来的） */
    @Override
    public void onPong(WsRequest request, byte[] bytes, ChannelContext ctx) {
        System.out.println("pong: " + new String(bytes));
    }
};
```

> 方法返回值支持：`WsResponse`、`byte[]`、`ByteBuffer`、`String`、`null`。`null` 表示不回写。

## 5. 主动推送（服务端 → 客户端）

业务 handler 中拿到 `ChannelContext` 之后，可以随时主动 send：

```java
@Override
public Object onText(WsRequest request, String text, ChannelContext ctx) {
    new Thread(() -> {
        for (int i = 0; i < 10; i++) {
            Tio.send(ctx, new WsResponse("tick-" + i));
            Thread.sleep(1000);
        }
    }).start();
    return null;
}
```

按 user / group 推送：

```java
Tio.bindUser(ctx, "user-" + userId);
Tio.sendToUser("user-123", new WsResponse("hello"));

Tio.bindGroup(ctx, "topic-news");
Tio.sendToGroup("topic-news", new WsResponse("breaking news!"));
```

主动断开：

```java
Tio.remove(ctx, "server kick");  // 服务端踢人
```

## 6. 握手阶段扩展

```java
IWsMsgHandler handler = new IWsMsgHandler() {
    /** 握手期：可以根据 Cookie / Token 拒绝升级 */
    @Override
    public HttpResponse handshake(HttpRequest req, HttpResponse resp, ChannelContext ctx) {
        String token = req.getParameter("token");
        if (token == null || !token.equals("secret")) {
            resp.setStatus(HttpResponseStatus.C401);
            return resp; // 返回非 null 会被原样发回，握手失败
        }
        return resp;   // 允许升级
    }

    /** 握手成功后回调 */
    @Override
    public void onAfterHandshaked(HttpRequest req, HttpResponse resp, ChannelContext ctx) {
        System.out.println("connected: " + ctx.getClientNode());
    }

    @Override
    public Object onText(WsRequest req, String text, ChannelContext ctx) {
        return "hi " + text;
    }
};
```

## 7. 协议行为（RFC 6455）

WebSocket 协议细节由框架处理，业务层不需要再关心：

| 场景 | 框架行为 |
| ---- | ---- |
| 握手校验 | 校验请求方法（GET）、`Upgrade: websocket`、`Connection: Upgrade`、`Sec-WebSocket-Version: 13`、`Sec-WebSocket-Key`（16 字节 base64）；不通过时返回 405 / 400 / 426 并断开连接 |
| 子协议协商 | 从客户端请求的 `Sec-WebSocket-Protocol` 中，按服务端声明的优先级挑一个回写 |
| 扩展协商 | 不支持任何扩展（如 `permessage-deflate`），响应中不会回写 `Sec-WebSocket-Extensions` |
| Ping | 收到 Ping **自动回 Pong**，负载原样带回 |
| Pong | 通过 `IWsMsgHandler#onPong` 回调给业务层 |
| Close | 收到 Close **自动回 Close**（状态码与对端保持一致），该帧发出后关闭 TCP 连接 |
| 分片消息 | 自动拼装；分片顺序不合法（续帧没有首帧、分片中途插入新数据帧）按协议错误处理 |
| 文本编码 | TEXT 帧按 RFC 要求强制 UTF-8 校验，非法序列按 1007 关闭 |
| 协议错误 | 非法 opcode、RSV 位未置零、控制帧超长或被分片、客户端帧未掩码等，按 RFC 对应的状态码（1002 / 1007 / 1009）回 Close 帧后断开 |

单条消息上限 512KB（`WsPacket.MAX_BODY_LENGTH`），超限按 1009 关闭。

### 7.1 服务端主动关闭

```java
import net.dreamlu.mica.net.websocket.common.Ws;
import net.dreamlu.mica.net.websocket.common.WsCloseCode;

// 优雅关闭：先发 Close 帧，等对端回应后再断开 TCP
Ws.close(ctx);                                     // 状态码 1000
Ws.close(ctx, WsCloseCode.GOING_AWAY, "服务端重启");

// 心跳探活
Ws.ping(ctx, "ping".getBytes());
```

> `Tio.remove(ctx, "...")` 是直接断 TCP，客户端会看到 1006 异常断开；
> `Ws.close` 走完整的关闭握手，客户端能拿到正确的关闭码。

## 8. WsServerStarter 构造选项

```java
// 仅端口
new WsServerStarter(8080, handler);

// 指定 IP + 端口
new WsServerStarter("0.0.0.0", 8080, handler);

// 自定义 HttpConfig（SSL / 最大帧 / Proxy Protocol）
HttpConfig cfg = new HttpConfig();
cfg.setSslConfig(SslConfig.forServer("keystore.jks", "pwd"));
new WsServerStarter(new Node(null, 443), cfg, handler);
```

## 9. SSL（HTTPS / WSS）

复用 mica-net 的 `SslConfig`，start 之后即支持 `wss://`：

```java
HttpConfig cfg = new HttpConfig();
cfg.setSslConfig(SslConfig.forServer("/path/to/keystore.jks", "password"));
new WsServerStarter(new Node(null, 443), cfg, handler).start();
```

## 10. 客户端

mica-net 暂未内置 WS 客户端，推荐使用：

- 浏览器原生 `WebSocket`
- Java 端可用 OkHttp / Java-WebSocket / Tyrus 等常规库连入

## 11. 常见问题

- **每连接线程模型**：每个 WS 连接占用一个 `ChannelContext`，handler 默认在 `groupExecutor` 上执行；高峰期请关注 `mica-net` 默认线程池。
- **心跳**：框架关闭了 TCP 心跳（`setHeartbeatTimeout(0)`），依靠 WebSocket 自身的 ping/pong 即可（框架收到 Ping 会自动回 Pong）。
- **二进制/大帧**：单条消息上限 512KB（`WsPacket.MAX_BODY_LENGTH`），超限按 1009 关闭；`HttpConfig` 里的 `maxLengthOfPostBody` 等参数只影响 HTTP 请求体，不影响 WS 帧。
- **跨域**：浏览器 WebSocket 不受 CORS 限制，但握手阶段可由 `handshake` 自行拦截。

## 12. 完整 Demo

```java
// 最完整的多功能示例直接抄这里：
// mica-net-http/src/test/java/.../WsExample.java    ← WebSocket 服务（可直接 main 启动）
// mica-net-http/src/test/java/.../SseExample.java   ← SSE 三种用法
// mica-net-http/src/test/java/.../RouterExample.java ← HTTP + 可与 WS 共存
//
// 上述几个示例展示了 mica-net-http 提供的完整服务能力，可以作为 WebSocket
// 服务端启动后再扩展业务的参考。
```

### 12.1 启动 WS 测试服务

`WsExample` 可以直接跑起来验证协议行为：

```bash
# 默认监听 8182，也可以传端口：java ... WsExample 9090
mvn -pl mica-net-http test-compile
java -cp mica-net-http/target/test-classes:... net.dreamlu.mica.net.http.test.WsExample
```

```javascript
// 浏览器控制台
let ws = new WebSocket('ws://localhost:8182');
ws.onmessage = e => console.log('recv', e.data);
ws.onopen = () => ws.send('hello');
```

| 发送内容 | 服务端行为 |
| ---- | ---- |
| `hello` | 回显 `echo:hello` |
| `ping` | 服务端发 Ping 帧，对端的 Pong 会打印到控制台 |
| `push` | 服务端连续推送 10 条 `tick-N` |
| `close` | 发起关闭握手，服务端与客户端互相回 Close 帧后断开 TCP |

连接串带 `?token=wrong` 会命中握手阶段的拦截，返回 401。

