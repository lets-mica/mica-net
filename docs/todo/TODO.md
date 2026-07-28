# mica-net 优化待办事项

> 最后核查：2026-07-27
>
> 原文中的内存、GC 和吞吐收益百分比尚无可复现基准支撑，已移除。后续优化以压测结果为准。

## 状态总览

### 已完成

- [x] TCP gather write 与原数组续写。
- [x] 消除发送热路径中的 `ConcurrentLinkedQueue.size()`。
- [x] 发送批次包数和字节数限制。
- [x] SSL 批次加密状态隔离。
- [x] 慢包检测滑动窗口。

### 部分完成

- [~] 读取路径拷贝：普通同步解码和 SSL 路径已避免深拷贝，Queue Decode 仍需复制。
- [~] 同步消息响应：响应侧已使用 `CompletableFuture`，但缺少完整的公开注册、发送和超时 API。

### 待实施

- [ ] ByteBuffer Pool 与 Queue Decode Buffer 所有权管理。
- [ ] 半包组合结构重新设计。
- [ ] 业务线程池隔离。
- [ ] DirectByteBuffer 混合策略。
- [ ] 统计模块分级控制。
- [ ] `ConcurrentLinkedQueue` Node 分配优化。
- [ ] 百万连接集合热点分析。
- [ ] 对象复用的协议级扩展点。
- [ ] 编解码性能监控和基准测试。

---

## P0：先建立基准并处理明确热点

### 1. 自动化性能基准

**目标：**

- 建立优化前后的可复现对照。
- 避免根据静态代码直接推导整体收益。

**场景：**

1. 1K 长连接、100K 小包/秒。
2. 发送队列分别积压 10、100、1K、10K 个包。
3. `useQueueDecode` 开启和关闭对比。
4. SSL 小包及 SSL 协议数据与业务数据交错。
5. 64 KB 以上大包。
6. 50% 半包比例。

**指标：**

- [ ] QPS/TPS。
- [ ] P50/P95/P99 延迟。
- [ ] CPU 使用率。
- [ ] 对象分配率和 GC 停顿。
- [ ] 堆内存和 DirectMemory。
- [ ] 队列深度和单批包数。

### 2. 发送队列遍历优化（已完成）

**当前实现：**

- `TcpSendRunnable.runTask()` 先 `poll()` 首包，不再调用 O(n) 的 `ConcurrentLinkedQueue.size()`。
- 非 SSL 批量路径使用单次 `poll()` 继续收集。
- SSL 路径只在检查下一包加密状态时使用 `peek()`。
- 单批最多 512 个包，初始列表容量为 32。
- 单批累计字节数仍受 `MAX_CAPACITY_MAX` 限制。
- 达到上限后的剩余包由写完成回调继续触发发送。

**测试：**

- [x] 单包路径不调用 `size()`。
- [x] 批量路径不调用 `size()`。
- [x] 包数上限后保留并续发剩余包。
- [x] 字节上限后保留剩余包。
- [x] SSL 明文到密文边界。
- [x] SSL 密文到明文边界。
- [ ] 补充实际 Socket 压测。

### 3. Queue Decode 拷贝（部分完成）

**当前事实：**

- `ReadCompletionHandler` 的读 Buffer 按连接复用，并非每次读取都重新分配。
- 普通同步解码直接消费当前 Buffer。
- SSL 路径使用 `slice()`，不再执行原文描述的完整深拷贝。
- `useQueueDecode=true` 时仍调用 `ByteBufferUtil.copy(buf)`，这是异步所有权隔离所必需的当前实现。

**后续步骤：**

- [ ] 记录 Queue Decode 场景的分配率和吞吐。
- [ ] 设计池化 Buffer 的获取、移交和释放协议。
- [ ] 覆盖解码积压、连接关闭、SSL 和异常路径。
- [ ] 增加泄漏检测与并发测试。

---

## P1：稳定性和可配置能力

### 4. 业务线程池隔离

**当前事实：**

- `decodeRunnable`、`handlerRunnable` 和 `sendRunnable` 当前都绑定 `tioExecutor`。
- `PacketHandlerMode.SINGLE_THREAD` 默认直接在解码线程执行 handler。

**设计要求：**

- [ ] 在 `TioConfig` 中增加可选 `businessExecutor`。
- [ ] 未配置时保持当前行为，确保兼容。
- [ ] 配置后仍保证单连接消息有序。
- [ ] 明确用户提供线程池是否由框架关闭。
- [ ] 增加阻塞 handler 和顺序性测试。

### 5. 统计模块分级

**当前事实：**

- 只有 `TioConfig.statOn`，尚无 `StatLevel`。
- `HandlerRunnable` 即使关闭统计，仍可能因监听器需要而计算耗时。

**后续步骤：**

- [ ] 先消除统计关闭且无监听器时的非必要计时。
- [ ] 设计 `OFF/BASIC/NORMAL/DETAILED`。
- [ ] 保留 `statOn` 的兼容映射。
- [ ] 对各级别进行性能对比。

### 6. DirectByteBuffer 混合策略

- [ ] 先建立大包 Heap/Direct 对照。
- [ ] 与 Buffer Pool 一并设计，避免频繁申请和释放 DirectMemory。
- [ ] 提供阈值、开关及 DirectMemory 监控。
- [ ] 小包默认继续使用 HeapByteBuffer。

### 7. 任务队列 Node 分配

**当前事实：**

- 发送、Queue Decode 和队列 Handler 仍使用 `ConcurrentLinkedQueue`。
- 发送路径的 O(n) `size()` 已消除，但入队 Node 分配仍存在。

**后续步骤：**

- [ ] 测量 Node 分配占总分配率的比例。
- [ ] 评估 MPSC 队列。
- [ ] 定义有界队列满载策略和背压语义。
- [ ] 分阶段替换，优先验证发送队列。

---

## P2：需要架构设计或明确数据后再实施

### 8. ByteBuffer Pool

- [ ] 定义 Pool 接口和大小分级。
- [ ] 明确 Heap/Direct 策略。
- [ ] 明确异步读、解码、SSL、异步写和关闭路径的所有权。
- [ ] 增加引用计数或等价生命周期机制。
- [ ] 增加泄漏检测和压力测试。

> 不建议直接复用 Netty 的完整分配器设计；应根据 mica-net 的 Buffer 生命周期裁剪。

### 9. `ByteBufferUtil.composite()` 零拷贝

**当前事实：**

- 现有实现仍为 `allocate + put`。
- 标准 `ByteBuffer` 返回类型不能表达多个离散 Buffer 的逻辑组合。

**后续步骤：**

- [ ] 先验证半包合并是否为实际热点。
- [ ] 评估新组合 Buffer 抽象。
- [ ] 评估修改 decode API 支持分段读取的兼容成本。
- [ ] 不再采用“保持 `ByteBuffer` 返回类型、仅修改内部实现”的不可行方案。

### 10. 集合分片

- [ ] 对 `Users`、`Groups`、`Ids`、`Tokens` 等维护结构进行百万连接压测。
- [ ] 使用分析结果确定热点，而不是预先引入分片 Map。
- [ ] 评估连接清理策略及额外索引的内存成本。

### 11. 对象复用

- [ ] 分析 Packet 创建热点。
- [ ] 优先由具体协议复用不可变心跳包和固定响应包。
- [ ] 不对可变 Packet 直接提供全局单例。
- [ ] 如需框架扩展点，明确 reset 和跨线程所有权规则。

### 12. 编解码性能监控

- [ ] 提供可关闭的编码、解码耗时采样。
- [ ] 增加可配置慢操作阈值。
- [ ] 避免默认对每个包进行高成本计时。
- [ ] 根据实际接入方式决定是否需要 JMX。

---

## 已完成项目说明

### TCP gather write

- `TcpSendRunnable` 已使用 `AsynchronousSocketChannel.write(ByteBuffer[], ...)`。
- `WriteCompletionHandler` 使用原数组的 `offset/length` 进行续写。
- 非 SSL 多包发送不再先合并到单个 ByteBuffer。
- SSL 明文批次仍需合并后交给当前加密接口。

### 同步消息响应（部分完成）

- `TioConfig.waitingResps` 已存在，类型为 `ConcurrentMap<Integer, CompletableFuture<Packet>>`。
- `HandlerRunnable` 收到带 `syncReqId` 的响应后会移除并完成对应 Future。
- 当前没有完整的 `Tio.sendAsync()`、`sendAndAwait()` 或等价公开 API，也没有框架内的 Future 注册和超时清理流程。
- 后续应先明确 API 需求，再决定是否继续完善；不再标记为“完全不存在”或“已经全部完成”。

### 慢包检测滑动窗口

- 实现类：`net.dreamlu.mica.net.core.stat.SlowPacketDetector`。
- `ChannelStat` 延迟创建检测器。
- `TcpDecodeRunnable` 在解码失败路径记录样本并按间隔检查。
- 当前配置位于 `TioConfig` 公共字段：

```java
tioConfig.enableSlowPacketDetection = true;
tioConfig.slowPacketWindowSize = 32;
tioConfig.slowPacketCheckInterval = 5;
tioConfig.maxDecodeFailCount = 10;
```

- [ ] 仍需补充优化前后的性能基准。

---

## 实施顺序建议

1. 建立自动化性能基准。
2. 业务线程池隔离。
3. 统计热路径减负和分级。
4. 根据分配率决定是否替换任务队列。
5. 根据 Queue Decode 基准决定是否引入 Buffer Pool。
6. 根据半包基准决定是否调整解码接口。

## 通用要求

1. 新配置保持默认行为兼容。
2. 并发优化必须覆盖关闭、异常和积压路径。
3. 性能收益必须给出同环境前后对照。
4. 更新实现时同步维护本文和 `performance.md`。
