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
| `UdpChannel` | UDP 网络通道：暴露 `send(Packet)`、`close(remark)`、`remoteAddress()`、`getConfig()`；是 UDP 中唯一的「上下文」 |
| `UdpHandler` | UDP 业务接口：`decode` / `encode` / `handler` 三段式 |
| `UdpConfig` / `UdpServerConfig` / `UdpClientConfig` | I/O 配置：`readBufferSize` 与可选的 `workerPool` |
| `UdpServer` / `UdpClient` | 启动器：`start()` 后阻塞读循环；`close()` 释放资源 |

> UDP 是无连接的，handler 中的 `UdpChannel` 只代表「这一次数据交换」，不是长连接。

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
                for (int i = 0; i < readableLength; i++) {
                    body[i] = buffer.get(position + i);
                }
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
                for (int i = 0; i < readableLength; i++) {
                    body[i] = buffer.get(position + i);
                }
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

> `client.send(packet)` 默认发往配置中的 `host:port`；如要发往其他地址，使用 `UdpChannel#send(packet, InetSocketAddress)`。

## 5. UdpConfig 常用项

```java
UdpServerConfig cfg = UdpServerConfig.builder()
    .port(9999)
    .readBufferSize(4096)                              // 单次 UDP 读取缓冲
    .workerPool(ThreadUtils.getGroupExecutor())        // 可选：复用业务线程池
    .build();
```

| 字段 | 默认 | 含义 |
| ---- | ---- | ---- |
| `port` | 必填（server） | 监听端口 |
| `host` | `127.0.0.1`（client） | 对端地址 |
| `readBufferSize` | `2048` | UDP 读取缓冲，建议 ≤ MTU（约 1500/9000） |
| `workerPool` | `null` | 注入业务线程池；为 null 时由 mica 自管理 |

## 6. 典型使用场景

- **IoT 上报**：设备 → 网关 UDP 接收，无需维护长连接。
- **服务发现**：节点之间周期性 UDP 心跳/广播。
- **查询/应答**：DNS 风格短报文。
- **日志/指标通道**：高并发、低开销、按需丢弃数据。

> 重要业务（订单、消息）请走 TCP；UDP 应用层需自行实现 ACK/重传与幂等。

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
