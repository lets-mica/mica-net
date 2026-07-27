# 核心组件性能与架构优化计划

> 最后核查：2026-07-27
>
> 本文只记录当前代码仍存在的性能问题，并同步已经落地的优化。所有收益均需通过基准测试确认，不再使用未经实测的固定百分比。

## 1. 内存管理与读取路径

### 1.1 Queue Decode 的 ByteBuffer 拷贝（待优化）

- **位置**：`ReadCompletionHandler.feedToDecodeRunnable()`。
- **当前状态**：
  - 普通同步解码直接使用当前读 Buffer，不进行深拷贝。
  - SSL 路径使用 `slice()` 共享底层数据，不再为每次读取创建完整副本。
  - 仅在 `useQueueDecode=true` 时，为避免异步解码期间底层读 Buffer 被复用，仍调用 `ByteBufferUtil.copy(buf)`。
- **后续方向**：基于池化 Buffer、引用计数或明确的所有权转移消除 Queue Decode 拷贝。
- **注意**：简单双缓冲无法覆盖解码长期落后、同时积压两个以上读取事件的情况。

### 1.2 ByteBuffer Pool（待设计）

- 读 Buffer 当前按连接创建并复用，只有读 Buffer 大小发生变化时才重新分配。
- Queue Decode 副本、SSL 批量合并及协议编码仍可能产生临时 Buffer。
- 引入池化前必须明确读取、解码、SSL、异步写入和连接关闭时的生命周期，并提供泄漏测试。

### 1.3 `ByteBufferUtil.composite()`（待重新设计）

- 当前实现仍为 `allocate + put` 的物理合并。
- 现有方法返回标准 `ByteBuffer`，无法直接表达由多个离散 Buffer 构成的逻辑视图。
- 真正零拷贝需要新的组合 Buffer 抽象，或调整解码接口支持分段读取；不能只修改现有方法内部实现。
- 在修改公开解码接口前，应先用半包基准确认该路径是否为实际热点。

## 2. 发送路径与任务队列

### 2.1 TCP 批量发送（已完成）

- 已使用 `AsynchronousSocketChannel.write(ByteBuffer[], ...)` 实现 gather write。
- 续写复用原始 Buffer 数组及 `offset/length`，不再构造剩余数组。
- 非 SSL 批量发送不再预先合并 ByteBuffer。
- SSL 因加密接口需要连续输入，当前仍会合并同一批次的明文 Buffer。

### 2.2 发送队列 O(n) `size()`（已完成）

- `TcpSendRunnable` 不再调用 `ConcurrentLinkedQueue.size()`。
- 单包路径先 `poll()`，批量路径有界收集：
  - 最大包数：512。
  - 最大字节数：约 1.3 MB。
  - 初始列表容量：32。
- 非 SSL 路径每个包只执行一次 `poll()`。
- SSL 路径通过 `peek()` 检查下一包的加密状态，防止明文与已加密数据进入同一批次。
- `TcpSendRunnableTest` 覆盖单包、批量、包数上限、字节上限、剩余包续发和 SSL 边界。

### 2.3 `ConcurrentLinkedQueue` Node 分配（待评估）

- `AbstractDecodeRunnable`、`AbstractSendRunnable` 和队列模式的 `HandlerRunnable` 仍使用 `ConcurrentLinkedQueue`。
- 替换为 MPSC 数组队列前，需要先定义队列满时的策略：拒绝、关闭连接、回退还是背压。
- 应先对 Node 分配率、队列深度和生产者竞争进行压测，再决定是否引入 JCTools 等依赖。

## 3. 线程池与统计

### 3.1 业务线程池隔离（待实施）

- `HandlerRunnable` 当前仍使用 `tioExecutor`。
- 默认 `PacketHandlerMode.SINGLE_THREAD` 会在解码线程中直接执行业务 handler。
- 独立业务线程池必须保持单连接消息顺序，并明确外部线程池的关闭责任。

### 3.2 统计分级（待实施）

- 当前只有 `TioConfig.statOn` 开关，没有 `StatLevel`。
- 优先处理关闭统计后仍执行的非必要计时，再评估 `OFF/BASIC/NORMAL/DETAILED` 分级。

## 4. 验证要求

后续性能改动至少覆盖以下场景：

1. 长连接高吞吐小包。
2. 发送队列持续积压。
3. Queue Decode。
4. SSL 明文与已加密协议数据交错。
5. 大包和高比例半包。

关注指标：

- QPS/TPS 与 P50/P95/P99 延迟。
- 网络线程 CPU 使用率。
- 对象分配率、堆内存和 GC 停顿。
- 队列深度与单批处理包数。
- DirectMemory（引入 DirectByteBuffer 后）。
