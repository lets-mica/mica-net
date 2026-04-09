# UDP DTLS 改造计划

> **状态**：暂不实现（2026-04-09 评估）
> **原因**：UDP 场景使用较少，当前能满足需求，暂无投入产出比

---

基于对 `mica-net` 当前源码的分析，UDP 的实现与 TCP 的实现有着良好的解耦。为了在不影响现有 TCP 实现的前提下加入 DTLS (Datagram Transport Layer Security) 支持，计划采用以下改造步骤和架构设计：

## 1. 独立 DTLS 状态管理 (不影响 TCP 的 SslFacadeContext)
* **现状**：TCP 使用了 `SslFacadeContext` 和标准的 `SSLEngine` 进行流式加解密。`UdpChannelContext.setUpSSL()` 中目前为禁用状态 (`// 暂不支持 SSL/TLS`)。
* **改造步骤**：
    * 不要复用或修改现有的 TCP `SslFacadeContext`。
    * 新增一个专为 UDP 设计的上下文类（例如 `UdpDtlsFacadeContext`），内部持有支持 DTLS 的引擎（如 Java 9+ 的 `SSLEngine` 或 BouncyCastle 的 `DTLSTransport`）。
    * 在 `UdpChannelContext.setUpSSL()` 中，判断如果开启了 SSL 且当前是 UDP，则初始化 `UdpDtlsFacadeContext` 并绑定到 `UdpChannelContext` 上。

## 2. 握手流程的异步改造 (处理丢包和乱序)
* **现状**：TCP 的 TLS 握手依赖底层 TCP 的可靠传输，按顺序收发握手包。
* **改造步骤**：
    * DTLS 握手在 UDP 环境下可能会丢包或乱序。`UdpDtlsFacadeContext` 内部需要维护一个状态机。
    * 当收到 `HANDSHAKING` 阶段的 DatagramPacket 时，交由 DTLS Engine 的 `unwrap` 处理。
    * 如果 `unwrap` 返回 `NEED_WRAP`，则立即生成响应的握手数据包，通过 `UdpChannelContext.datagramChannel` 发送。
    * **重传机制**：必须利用 `TioConfig` 的定时任务（如 `tioExecutor`）实现 DTLS 握手超时重传机制。如果在一个时间窗口内没有收到预期的握手响应，需要重新触发引擎发送上一轮的握手包。

## 3. 数据接收拦截 (解密)
* **现状**：数据到达后进入 `UdpChannelContext.handleReceivedData(ByteBuffer buffer)`，然后直接丢给 `UdpDecodeRunnable` 解码成 `Packet`。
* **改造步骤**：
    * 在进入 `UdpDecodeRunnable` 之前进行拦截。判断如果启用了 DTLS，则将原始 UDP `ByteBuffer` 传入 DTLS Engine 的 `unwrap()`。
    * 如果解密成功（产生 App Data），再将**解密后的明文 ByteBuffer** 丢给现有的 `UdpDecodeRunnable`。
    * 如果解密出的是握手数据，则直接在内部消化并推进握手状态，**不**将数据传递给应用层的业务解码器。
    * **异常处理**：如果 DTLS 报文校验失败（如防重放攻击校验失败、MAC 错误），直接丢弃该数据包并打印警告日志，**千万不要像 TCP 那样直接关闭整个 Channel**。

## 4. 数据发送拦截 (加密)
* **现状**：`UdpSendRunnable.sendPacket()` 负责将 `Packet` 编码为 `ByteBuffer` 然后通过 Channel 发送。目前里面的 SSL 加密逻辑实际上是 TCP 的占位符，不适用于 UDP。
* **改造步骤**：
    * 在 `UdpSendRunnable` 中，编码出明文 `ByteBuffer` 后，判断是否为 DTLS。
    * 如果是 DTLS，将明文 `ByteBuffer` 传入 DTLS Engine 的 `wrap()` 进行加密。
    * 将加密后的 `ByteBuffer` 通过 `DatagramChannel` 发送出去。
    * **MTU 限制**：由于标准 DTLS 不会自动对应用数据进行分片，必须确保加密后的包大小不超过网络 MTU。可以在加密前增加一层校验，超长则抛出异常或在应用层切片。

## 5. 总结与设计原则
整体原则是**”旁路拦截，独立上下文”**。将 DTLS 的引擎（`unwrap`/`wrap`）作为拦截器，插入到 `DatagramChannel` 的读写与 `UdpDecodeRunnable`/`UdpSendRunnable` 之间，完全避开 `TcpChannelContext` 和现有的 TCP TLS 流程。这需要 JDK 9+ 的支持，或者引入 BouncyCastle 作为底层 Provider。

---

## ⚠️ 评估结论（2026-04-09）

### 重大障碍

| 障碍 | 说明 |
|------|------|
| **Java 版本不兼容** | 项目最低支持 Java 1.8，但 `SSLEngine` 的 DTLS 模式需要 JDK 9+。若要支持需引入 BouncyCastle（约 5MB 依赖），或提升最低版本要求 |

### 需澄清点

1. **MTU 分片**：DTLS 1.2 规范定义了分片机制，但 Java 原生 `SSLEngine` 不自动处理，需应用层实现
2. **握手状态机**：重传机制、epoch 切换、防重放等均需自行实现，工作量较大
3. **useQueueDecode=false 路径**：需同时覆盖队列模式和直接处理两条数据路径的 DTLS 解密拦截

### 后续若要重启

1. 确定 Java 版本策略（BouncyCastle 或升 JDK）
2. 设计 MTU 分片处理方案
3. 细化 `UdpDtlsFacadeContext` 与现有 Runnables 的拦截点
