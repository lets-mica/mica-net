# mica-net TCP 使用文档

> mica-net 2.0.x 基于 Java NIO `AsynchronousSocketChannel` 实现 TCP 异步通信，本文档聚焦**最快上手**与**核心 API**，不展开内部实现。

## 1. 引入依赖

```xml
<dependency>
    <groupId>net.dreamlu</groupId>
    <artifactId>mica-net-core</artifactId>
    <version>${mica-net.version}</version>
</dependency>
```

> mica-net 2.0.0 之后包名已统一为 `net.dreamlu.mica.net.*`，可与原版 t-io 同时存在。

## 2. 核心概念

- **TcpHandler**：TCP 业务接口，统一了 `decode/encode/handler` 三段式 + 可选 `heartbeatPacket`。`TcpServerHandler`、`TcpClientHandler` 是分别面向 server / client 的特化子接口。
- **ChannelContext**：每个 TCP 连接对应一个上下文，提供 `send/bSend/close` 等方法（v2.0.7 实现 `NetChannel`，可与 UDP 共享抽象）。
- **Packet**：业务包抽象，内置 `IgnorePacket`（跳过 handler）、`EncodedPacket`（直接携带已编码字节）两种便捷实现。
- **Tio**：对外 API 入口，封装 `send/close/bind/unbind/schedule` 等。
- **TioServerConfig / TioClientConfig**：分别承载服务端 / 客户端的线程、统计、心跳、SSL、backlog 等参数。

## 3. 最小服务端示例

定义一个**定长帧解码器**：

```java
public class FixedLengthCodec {
    private final int length;

    public FixedLengthCodec(int length) {
        this.length = length;
    }

    public Packet decode(ByteBuffer buffer, int readableLength) {
        if (readableLength < length) {
            return null; // 半包，等待更多数据
        }
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new EncodedPacket(bytes); // 偷懒：直接用 EncodedPacket 携带解码后的字节
    }

    public ByteBuffer encode(Packet packet) {
        return ByteBuffer.wrap(((EncodedPacket) packet).getBytes());
    }
}
```

实现服务端 handler：

```java
public class DemoServerHandler implements TcpServerHandler {
    private final FixedLengthCodec codec;

    public DemoServerHandler(FixedLengthCodec codec) {
        this.codec = codec;
    }

    @Override
    public Packet decode(ByteBuffer buffer, int limit, int position,
                        int readableLength, ChannelContext ctx) throws TioDecodeException {
        return codec.decode(buffer, readableLength);
    }

    @Override
    public ByteBuffer encode(Packet packet, TioConfig config, ChannelContext ctx) {
        return codec.encode(packet);
    }

    @Override
    public void handler(Packet packet, ChannelContext ctx) throws Exception {
        byte[] body = ((EncodedPacket) packet).getBytes();
        System.out.println("server recv: " + new String(body, StandardCharsets.UTF_8));
        // 回写
        Tio.send(ctx, new EncodedPacket(
            ("echo:" + System.nanoTime()).getBytes(StandardCharsets.UTF_8)));
    }
}
```

启动服务端：

```java
public class TcpServerTest {
    public static void main(String[] args) throws IOException {
        int length = ("mica:" + System.nanoTime()).getBytes(StandardCharsets.UTF_8).length;
        FixedLengthCodec codec = new FixedLengthCodec(length);
        TioServerHandler handler = new DemoServerHandler(codec);

        TioServerConfig config = new TioServerConfig(handler, new DefaultTioServerListener());
        config.debug = true;

        TioServer server = new TioServer(502, config);
        server.start();
    }
}
```

## 4. 最小客户端示例

```java
public class DemoClientHandler implements TcpClientHandler {
    private final FixedLengthCodec codec;

    public DemoClientHandler(FixedLengthCodec codec) {
        this.codec = codec;
    }

    @Override
    public Packet heartbeatPacket(ChannelContext ctx) {
        // 自定义心跳包，返回 null 表示不发送
        return null;
    }

    @Override
    public Packet decode(ByteBuffer buffer, int limit, int position,
                        int readableLength, ChannelContext ctx) throws TioDecodeException {
        return codec.decode(buffer, readableLength);
    }

    @Override
    public ByteBuffer encode(Packet packet, TioConfig config, ChannelContext ctx) {
        return codec.encode(packet);
    }

    @Override
    public void handler(Packet packet, ChannelContext ctx) throws Exception {
        byte[] body = ((EncodedPacket) packet).getBytes();
        System.out.println("client recv: " + new String(body, StandardCharsets.UTF_8));
    }
}
```

启动客户端：

```java
public class TcpClientTest {
    public static void main(String[] args) throws Exception {
        int length = ("mica:" + System.nanoTime()).getBytes(StandardCharsets.UTF_8).length;
        FixedLengthCodec codec = new FixedLengthCodec(length);

        TioClientConfig config = new TioClientConfig(
            new DemoClientHandler(codec), new DefaultTioClientListener());
        config.setReconnConf(new ReconnConf()); // 开启自动重连
        config.debug = true;

        TioClient client = new TioClient(config);
        ClientChannelContext ctx = client.connect(new Node("127.0.0.1", 502));

        client.schedule(() -> {
            for (int i = 0; i < 1000; i++) {
                String msg = "mica:" + System.nanoTime();
                Tio.send(ctx, new EncodedPacket(msg.getBytes(StandardCharsets.UTF_8)));
            }
        }, 3000);
    }
}
```

## 5. 常用 API

| 场景 | 调用 | 备注 |
| ---- | ---- | ---- |
| 单发 | `Tio.send(ctx, packet)` | 同步入队 |
| 阻塞发 | `Tio.bSend(ctx, packet)` | 等发送完毕 |
| 群发 | `Tio.sendToAll(packet, filter)` / `ServerChannelContext.sendToAll(...)` | 按 filter 过滤 |
| 绑定 UserId | `Tio.bindUser(ctx, "u001")` | 一个连接绑定一个 user |
| 群组 | `Tio.bindGroup(ctx, "grp1")` | 一个连接可加入多个群组 |
| 按 user 发送 | `Tio.sendToUser("u001", packet)` | 需要先 bindUser |
| 按 group 发送 | `Tio.sendToGroup("grp1", packet)` | 需要先 bindGroup |
| 客户端断开 | `Tio.close(ctx, remark)` | 主动 close 客户端触发重连逻辑 |
| 服务端断开 | `Tio.remove(ctx, remark)` | 服务端踢人 |
| 调度任务 | `tioServer.schedule(r, 3000)` | 基于 HashedWheelTimer，无需额外线程 |

## 6. 常用配置（TioConfig）

| 字段 | 默认值 | 含义 |
| ---- | ---- | ---- |
| `heartbeatTimeout` | `120000ms` | 心跳超时，超时按 `heartbeatTimeoutStrategy` 决策 |
| `heartbeatTimeoutStrategy` | `NO` | `NO`/`SEND_PING`/`DISCONNECT_RECONNECT` |
| `useQueueDecode` | `true` | 解码是走线程池队列还是直跑 |
| `useQueueSend` | `true` | 发送是否入队（`false` 可减少一次线程切换） |
| `backlog` | `50` | 服务端 accept backlog |
| `debug` | `false` | 是否输出详细日志 |

```java
TioServerConfig config = new TioServerConfig(handler, listener);
config.setHeartbeatTimeout(60_000);
config.heartbeatTimeoutStrategy = HeartbeatTimeoutStrategy.SEND_PING;
config.useQueueDecode = true;
config.useQueueSend = true;
config.backlog = 128;
```

## 7. SSL/TLS（可选）

```java
SslConfig ssl = SslConfig.forServer("/path/to/keystore.jks", "password");
config.setSslConfig(ssl);

// 解码/编码无需感知 SSL，TcpHandler 接口保持透明
TcpHandler handler = new DemoServerHandler(codec);
```

- 客户端证书：`SslConfig.forClient("classpath:client.crt")`
- 自定义 TLS 版本与加密套件：实现 `SSLEngineCustomizer` 后注册到 `SslConfig`
- 支持 `PKCS12` 证书、双向认证以及 `PROXY protocol` + SSL 复合场景

## 8. PROXY Protocol（可选）

nginx / ELB 等网关启用 `proxy_protocol on;` 时，网关会在 TCP 前插入一个 PROXY 头；服务端开启该开关后可读取真实 client IP：

```java
config.setProxyProtocolDecoder(true); // 默认开启则无需配置
```

- 同时支持 v1、v2 两种格式
- PROXY header 与业务协议共存，未带 PROXY header 的连接不会断开，方便灰度

## 9. 主动断开语义

| 端 | 推荐 API | 触发链路 |
| -- | -------- | -------- |
| 客户端主动 close | `Tio.close(ctx, remark)` | 会触发连接关闭 + 客户端按照 `ReconnConf` 自动重连 |
| 服务端主动踢人 | `Tio.remove(ctx, remark)` | 服务端释放连接但不会重连 |

## 10. 集群 & 节点

```java
config.setClusterConfig(new ClusterConfig()
    .setNodes("10.0.0.1:6789", "10.0.0.2:6789")
    .setOnMessage(...) // 监听集群广播
);
```

用于多实例间 group / user 状态同步、消息广播、节点上下线通知。

## 11. 常见问题

- **半包/粘包处理**：在 `decode` 中可读长度不够时返回 `null`，框架会自动拼接 `lastByteBuffer` 等待下次数据；不够一帧应**不要消费** `ByteBuffer`。
- **内存暴涨**：默认 `useQueueSend=true` 已开启批量发送；如对延迟更敏感可关闭，需结合业务评估。
- **慢包攻击防护**：框架已内置滑动窗口检测；可在 `TioConfig#setSlowPacketThreshold(...)` 调整阈值。

> 更多内部细节可参考 `README.md` 的「接收处理核心逻辑」与「发送处理核心逻辑」。
