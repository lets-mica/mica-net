# 核心组件性能与架构优化计划

基于对 `mica-net-core` 和 `mica-net-utils` 的深度代码走查，梳理出以下需要在未来版本中优先解决的性能瓶颈、并发 Bug 以及架构优化点：

## 1. 内存管理与 I/O 零拷贝优化 (极高优先级 - 降低 GC 压力)
* **缺失 Vectorized I/O (分散/聚集 I/O)**：
  * **位置**：`TcpSendRunnable.sendByteBuffers(ByteBuffer[] byteBuffers, ...)`。
  * **现状**：当前在批量发送多个包时，会重新申请一个大的 `ByteBuffer` 并把所有小 Buffer 数据 copy 进去再发送。
  * **优化**：Java AIO `AsynchronousSocketChannel` 原生支持 `write(ByteBuffer[] srcs, ...)`。直接传递数组，将合并工作交给 OS 内核的 Gathering Write，彻底消除用户态内存分配和拷贝。
* **深拷贝引发的 Young GC 压力**：
  * **位置**：`ReadCompletionHandler.completed()`。
  * **现状**：开启 `useQueueDecode` 或 SSL 时，每次读取都会触发 `ByteBufferUtil.copy(readByteBuffer)` 进行全量深拷贝。
  * **优化**：引入 `ByteBufferPool` 池化技术。
* **引入逻辑 CompositeByteBuffer**：
  * **现状**：处理 TCP 半包/粘包时，通过申请新 Buffer 进行物理拼凑。
  * **优化**：参考 Netty 设计无锁/少锁的 `CompositeByteBuffer`，通过维护指针数组实现逻辑上的 Buffer 组合，避免物理拷贝。

## 2. 并发与锁的极致优化 (中高优先级 - 提升极限吞吐量)
* **任务队列的 Node 分配开销**：
  * **位置**：`AbstractDecodeRunnable`、`AbstractSendRunnable` 中的 `msgQueue` (`ConcurrentLinkedQueue`)。
  * **现状**：`ConcurrentLinkedQueue` 每次 `add()` 都会分配一个新的 `Node` 对象，在百万并发下会产生海量零碎对象。
  * **优化**：引入类似 JCTools 的无锁数组队列（如 `MpscArrayQueue`，多生产者单消费者），大幅降低对象分配率。
* **分布式 ID 生成器的锁竞争**：
  * **位置**：`mica-net-utils` 模块中的 `Snowflake.nextId()`。
  * **现状**：使用了 `synchronized` 关键字修饰。如果每个网络包都依赖此方法生成 ID，该方法将成为全局串行瓶颈。
  * **优化**：使用 `AtomicLong` 的 CAS 操作改写雪花算法，实现无锁化 (Lock-free)。
* **任务调度的锁开销**：
  * **位置**：`AbstractSynRunnable` 与 `SynThreadPoolExecutor`。
  * **现状**：为了防止同一连接的任务被多线程并发执行，使用了 `ReentrantLock.tryLock()` 控制。重量级锁在极限并发下上下文切换开销大。
  * **优化**：使用 `AtomicInteger` 状态位 (`0` 表示空闲，`1` 表示运行中) 进行 CAS 操作进行轻量级互斥。