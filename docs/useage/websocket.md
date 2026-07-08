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
| `IWsMsgHandler` | 业务接口：`onText` / `onBytes` / `onClose` / `handshake` / `onAfterHandshaked` |
| `WsRequest` | 携带连接信息：`ChannelContext`、`headers`、`pathParam` 等 |
| `ChannelContext` | TCP 连接上下文，可绑定 `userId` / `group` / `send` / `close` |
| `Opcode` | 区分 TEXT/BINARY/CLOSE/PING/PONG 等 |

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

## 7. WsServerStarter 构造选项

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

## 8. SSL（HTTPS / WSS）

复用 mica-net 的 `SslConfig`，start 之后即支持 `wss://`：

```java
HttpConfig cfg = new HttpConfig();
cfg.setSslConfig(SslConfig.forServer("/path/to/keystore.jks", "password"));
new WsServerStarter(new Node(null, 443), cfg, handler).start();
```

## 9. 客户端

mica-net 暂未内置 WS 客户端，推荐使用：

- 浏览器原生 `WebSocket`
- Java 端可用 OkHttp / Java-WebSocket / Tyrus 等常规库连入

## 10. 常见问题

- **每连接线程模型**：每个 WS 连接占用一个 `ChannelContext`，handler 默认在 `groupExecutor` 上执行；高峰期请关注 `mica-net` 默认线程池。
- **心跳**：框架关闭了 TCP 心跳（`setHeartbeatTimeout(0)`），依靠 WebSocket 自身的 ping/pong 即可。
- **二进制/大帧**：`HttpConfig` 默认支持较大帧；如确需更大可通过 `HttpConfig` 调整 `maxBodyLength` 等参数。
- **跨域**：浏览器 WebSocket 不受 CORS 限制，但握手阶段可由 `handshake` 自行拦截。

## 11. 完整 Demo

```java
// 最完整的多功能示例直接抄这里：
// mica-net-http/src/test/java/.../SseExample.java   ← SSE 三种用法
// mica-net-http/src/test/java/.../RouterExample.java ← HTTP + 可与 WS 共存
//
// 上述两个示例展示了 mica-net-http 提供的完整服务能力，可以作为 WebSocket
// 服务端启动后再扩展业务的参考。
```
