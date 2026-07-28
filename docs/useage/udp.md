# mica-net UDP 使用文档

> mica-net 内置标准 NIO UDP，基于 `DatagramChannel` + `UdpChannel/UdpHandler`，不依赖 TCP 的 `ChannelContext` 重量级状态机，更轻量。

## 1. 引入依赖

UDP 与 TCP 共用 `mica-net-core`，无需额外模块：

```xml
<dependency>
    <groupId>net.dreamlu</groupId>
    <artifactId>mica-net-core</artifactId>
    <version>${mica-net.version}</version>
</dependency>
```

## 2. 核心概念

| 名称 | 作用 |
| ---- | ---- |
| `UdpChannel` | UDP 网络通道：暴露 `send(Packet)`、`remoteAddress()`、`getConfig()`；是 UDP 中唯一的「上下文」 |
| `UdpHandler` | UDP 业务接口：`decode` / `encode` / `handler` 三段式 |
| `UdpConfig` / `UdpServerConfig` / `UdpClientConfig` | I/O 配置：`readBufferSize`、可选 `workerPool`，以及 server/client 专有项 |
| `UdpServer` / `UdpClient` | 启动器：`start()` 启动后台 I/O 线程后立即返回；`close()` 释放资源 |

> UDP 本身无连接。服务端会按对端 `InetSocketAddress` **复用** `UdpServerChannel`（便于维护 per-peer 状态），并按 `peerIdleTimeoutMs` / `maxPeers` 约束会话；`close()` 或 `UdpChannel#close(remark)` 会移除该会话。客户端始终对应配置中的单一目标地址。
>
> 同一对端的 `handler` **串行**执行（per-peer 队列）；不同对端仍可并行。回包经有界发送队列由单线程写出。

### 解码约定

- 以 **单个 datagram** 为界循环调用 `decode`，一包可含多帧。
- **不会**跨 datagram 自动拼接半包；`decode` 返回 `null` 即丢弃本 datagram 剩余数据。
- 返回 `Packet` 后应推进 `ByteBuffer#position`；若不推进，框架为防死循环只处理一帧。
- `readBufferSize` 小于实际 datagram 时可能被截断（框架会打 warn）。

## 3. 最简单的服务端

任何返回字节数组的协议都可以，下面用"UTF-8 文本无分隔"做演示，业务中可以替换为长度前缀、protobuf 等等。

```java
import net.dreamlu.mica.net.core.intf.*;
import net.dreamlu.mica.net.core.udp.UdpConfig;
import net.dreamlu.mica.net.server.udp.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class UdpEchoServer {
    public static void main(String[] args) throws Exception {
        UdpHandler handler = new UdpHandler() {
            @Override
            public Packet decode(ByteBuffer buffer, int limit, int position,
                                int readableLength, UdpChannel ctx) {
                // 数据不足一帧返回 null；这里直接以"缓冲区剩余全部作为一包"为例
                if (readableLength <= 0) return null;
                byte[] body = new byte[readableLength];
                buffer.position(position);
                buffer.get(body);
                return new TextPacket(new String(body, StandardCharsets.UTF_8));
            }

            @Override
            public ByteBuffer encode(Packet packet, UdpConfig config, UdpChannel ctx) {
                String text = ((TextPacket) packet).getBody();
                return ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public void handler(Packet packet, UdpChannel channel) {
                String msg = ((TextPacket) packet).getBody();
                channel.send(new TextPacket("echo:" + msg));
            }
        };

        UdpServer server = new UdpServer(
            UdpServerConfig.builder().port(9999).build(),
            handler
        );
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
    }

    /** 自定义业务包：只需实现 Packet 接口 */
    static class TextPacket extends Packet {
        private static final long serialVersionUID = 1L;
        private final String body;
        TextPacket(String body) { this.body = body; }
        String getBody() { return body; }
        @Override public String logstr() { return "TextPacket(" + body + ")"; }
    }
}
```

启动后用 `nc -u 127.0.0.1 9999` 或者 `UdpClient` 都可以打过来。

## 4. 最简单的客户端

```java
import net.dreamlu.mica.net.client.udp.*;
import net.dreamlu.mica.net.core.udp.UdpConfig;
import net.dreamlu.mica.net.core.intf.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class UdpEchoClient {
    public static void main(String[] args) throws Exception {
        UdpHandler handler = new UdpHandler() {
            @Override
            public Packet decode(ByteBuffer buffer, int limit, int position,
                                int readableLength, UdpChannel ctx) {
                if (readableLength <= 0) return null;
                byte[] body = new byte[readableLength];
                buffer.position(position);
                buffer.get(body);
                return new UdpEchoServer.TextPacket(new String(body, StandardCharsets.UTF_8));
            }
            @Override public ByteBuffer encode(Packet p, UdpConfig c, UdpChannel ch) {
                return ByteBuffer.wrap(((UdpEchoServer.TextPacket) p).getBody()
                    .getBytes(StandardCharsets.UTF_8));
            }
            @Override public void handler(Packet p, UdpChannel ch) {
                System.out.println("recv: " + ((UdpEchoServer.TextPacket) p).getBody());
            }
        };

        UdpClient client = new UdpClient(
            UdpClientConfig.builder().host("127.0.0.1").port(9999).build(),
            handler
        );
        client.start();

        client.send(new UdpEchoServer.TextPacket("hello"));
        Thread.sleep(1000);
        client.close();
    }
}
```

> `client.send(packet)` / `UdpChannel#send(packet)` 均发往客户端配置中的 `host:port`。服务端回包则发往该会话的 `remoteAddress()`。当前不提供「同一 channel 改发其他地址」的 API。

## 5. UdpConfig 常用项

```java
UdpServerConfig cfg = UdpServerConfig.builder()
    .port(9999)
    .readBufferSize(4096)                              // 单次 UDP 读取缓冲
    .workerThreads(8)                                  // 默认池线程数（未注入 workerPool 时）
    .peerIdleTimeoutMs(300_000)                        // 对端空闲淘汰；0 表示不按空闲淘汰
    .maxPeers(10_000)                                  // 对端会话上限，满则丢弃新对端报文
    .sendQueueCapacity(4096)                           // 回包发送队列；满则 send 返回 false
    .workerPool(ThreadUtils.getGroupExecutor())        // 可选：复用业务线程池
    .build();

UdpClientConfig clientCfg = UdpClientConfig.builder()
    .host("127.0.0.1")
    .port(9999)
    .sendQueueCapacity(1024)                           // 发送队列满时 send 返回 false
    .build();
```

| 字段 | 默认 | 含义 |
| ---- | ---- | ---- |
| `port` | 必填（server） | 监听端口 |
| `host` | `127.0.0.1`（client） | 对端地址 |
| `readBufferSize` | `2048` | UDP 读取缓冲；过小会导致 datagram 截断 |
| `workerPool` | `null` | 注入业务线程池；为 null 时由 mica 自管理 |
| `workerThreads` | `max(2, CPUs*2)`（server） | 默认业务池线程数 |
| `peerIdleTimeoutMs` | `300000`（server） | 对端会话空闲超时；`0` 关闭空闲淘汰 |
| `maxPeers` | `10000`（server） | 对端会话上限 |
| `sendQueueCapacity` | `4096`（server）/ `1024`（client） | 发送队列容量，满则 `send` 返回 `false` |

## 6. 典型使用场景

- **IoT 上报**：设备 → 网关 UDP 接收，无需维护长连接。
- **服务发现**：节点之间周期性 UDP 心跳/广播。
- **查询/应答**：DNS 风格短报文。
- **日志/指标通道**：高并发、低开销、按需丢弃数据。

> 重要业务（订单、消息）请走 TCP；UDP 应用层需自行实现 ACK/重传与幂等。
> 非阻塞发送在内核缓冲区满时可能丢包（`send` 返回 `false` / 客户端累计 `getDroppedSendCount()`）。

## 7. 完整 Demo

仓库自带的 `mica-net-core/src/test/java/net/dreamlu/mica/net/core/udp/UdpDemo.java` 演示了"长度前缀 + UTF-8 文本"的完整协议（含 `runBoth` 模式：同一 JVM 启 server + client 互相收发后退出），可作为更复杂协议的起点。

```bash
# 编译测试代码
mvn -pl mica-net-core test-compile -DskipTests

# 同进程启 server 和 client
java -cp mica-net-core/target/test-classes:mica-net-core/target/classes:... \
     net.dreamlu.mica.net.core.udp.UdpDemo

# 分别启
java ... net.dreamlu.mica.net.core.udp.UdpDemo server
java ... net.dreamlu.mica.net.core.udp.UdpDemo client
```
