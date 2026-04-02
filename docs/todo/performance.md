# 核心组件性能与架构优化计划

基于对 `mica-net-core` 和 `mica-net-utils` 的深度代码走查，梳理出以下需要在未来版本中优先解决的性能瓶颈、并发 Bug 以及架构优化点：

## 1. 内存管理与 I/O 零拷贝优化 (极高优先级 - 降低 GC 压力)
* **深拷贝引发的 Young GC 压力**：
  * **位置**：`ReadCompletionHandler.completed()`。
  * **现状**：开启 `useQueueDecode` 或 SSL 时，每次读取都会触发 `ByteBufferUtil.copy(readByteBuffer)` 进行全量深拷贝。
  * **优化**：引入 `ByteBufferPool` 池化技术。
* **ByteBufferUtil.composite() 应改造为零拷贝实现**：
  * **位置**：`org.tio.utils.buffer.ByteBufferUtil.composite(ByteBuffer, ByteBuffer)`。
  * **现状**：当前实现为 `allocate() + put()` 全量物理拷贝，高频调用时造成 GC 压力。
  * **优化**：参考 Netty 的 `CompositeByteBuffer`，维护 ByteBuffer 数组 + 偏移量/长度信息，实现逻辑上的组合。读取时通过指针遍历而非拷贝，数据仅在真正需要连续内存时才合并。

## 2. 并发与锁的极致优化 (中高优先级 - 提升极限吞吐量)
* **任务队列的 Node 分配开销**：
  * **位置**：`AbstractDecodeRunnable`、`AbstractSendRunnable` 中的 `msgQueue` (`ConcurrentLinkedQueue`)。
  * **现状**：`ConcurrentLinkedQueue` 每次 `add()` 都会分配一个新的 `Node` 对象，在百万并发下会产生海量零碎对象。
  * **优化**：引入类似 JCTools 的无锁数组队列（如 `MpscArrayQueue`，多生产者单消费者），大幅降低对象分配率。
