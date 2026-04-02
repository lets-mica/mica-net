# mica-net 优化待办事项

> 基于 2026-02-11 全局架构、内存和性能分析
>
> 优化目标：
> - 内存占用降低 40-50%
> - GC 停顿时间减少 60%+
> - 吞吐量提升 30-50%

---

## P0 - 立即优化（收益显著）

### 1. ByteBuffer 池化机制

**问题描述：**
- `ReadCompletionHandler` 和 `SendRunnable` 中频繁调用 `ByteBuffer.allocate()`
- 每次读取都进行深拷贝，导致高频 GC
- 堆内存分配压力大，Young GC 频繁

**优化方案：**
- 引入 ByteBuffer Pool（参考 Netty PooledByteBufAllocator）
- 使用 ThreadLocal 缓存常用大小的 ByteBuffer
- 实现分级池化：Small (< 8KB), Medium (8-64KB), Large (> 64KB)
- 增加引用计数机制管理生命周期

**关键代码位置：**
- `mica-net-core/src/main/java/org/tio/core/ReadCompletionHandler.java` (深拷贝位于 ~246-258)
- `mica-net-core/src/main/java/org/tio/core/tcp/TcpSendRunnable.java`
- 新增 `mica-net-core/src/main/java/org/tio/core/buffer/ByteBufferPool.java`

**预期收益：**
- 减少 50-70% 的内存分配
- Young GC 频率降低 60%+

**实施步骤：**
- [ ] 设计 ByteBufferPool 接口和实现类
- [ ] 实现分级池化策略（Small/Medium/Large）
- [ ] 在 TioConfig 中添加池化配置选项
- [ ] 重构 ReadCompletionHandler 使用池化 Buffer
- [ ] 重构 SendRunnable 使用池化 Buffer
- [ ] 添加引用计数和泄漏检测机制
- [ ] 编写单元测试和压力测试

---

### 2. 消除 ReadCompletionHandler 中的内存拷贝

**问题描述：**
```java
// ReadCompletionHandler.completed()
if (useQueueDecode || sslFacadeContext != null) {
    newByteBuffer = ByteBufferUtil.copy(readByteBuffer);  // ⚠️ 性能杀手
}
```
- 每次读取都深拷贝整个 buffer
- 读取路径是最高频操作，影响整体性能

**优化方案：**

**方案 1：引用计数 + 读写分离**
- readByteBuffer 设置为只读，传递引用而非拷贝
- DecodeRunnable 完成后释放引用计数
- 配合 P0-1 的池化机制实现

**方案 2：双缓冲策略**
- 每个连接维护两个 buffer 交替使用
- 读取时切换 buffer，解码使用另一个
- 适用于不需要池化的简化场景

**关键代码位置：**
- `mica-net-core/src/main/java/org/tio/core/ReadCompletionHandler.java` (~246-258)
- `mica-net-core/src/main/java/org/tio/core/ChannelContext.java`（增加双缓冲字段）

**预期收益：**
- 读取路径性能提升 30-40%
- 内存拷贝开销降低 90%+

**实施步骤：**
- [ ] 评估方案 1 vs 方案 2（建议方案 1 配合池化）
- [ ] 修改 ReadCompletionHandler 逻辑
- [ ] 在 ChannelContext 中增加引用计数管理
- [ ] 确保 DecodeRunnable 正确释放引用
- [ ] 测试并发场景下的线程安全性
- [ ] 压力测试验证性能提升

---

### 3. ByteBufferUtil.composite() 零拷贝改造

**问题描述：**
```java
// ByteBufferUtil.composite() 现有实现
ByteBuffer ret = ByteBuffer.allocate(capacity);
ret.put(byteBuffer1);
ret.put(byteBuffer2);  // 物理拷贝
```
- `ByteBufferUtil.composite()` 已存在，但实现为 `allocate + put` 全量拷贝
- 半包场景频繁时成为性能热点

**优化方案：**
- 不新增类，改造现有 `ByteBufferUtil.composite()` 方法
- 内部维护 ByteBuffer 数组 + 偏移量/长度信息（逻辑组合）
- 提供统一的读写接口，数据仅在真正需要连续内存时才合并
- 参考 Netty CompositeByteBuf 设计思路

**关键代码位置：**
- `mica-net-utils/src/main/java/org/tio/utils/buffer/ByteBufferUtil.java`（现有 composite 方法）
- `mica-net-core/src/main/java/org/tio/core/tcp/TcpDecodeRunnable.java`（半包处理调用点）

**预期收益：**
- 半包场景性能提升 20-30%
- 减少内存分配和 GC 压力

**实施步骤：**
- [ ] 设计逻辑组合结构（维护 ByteBuffer 数组 + 偏移量）
- [ ] 改造 ByteBufferUtil.composite() 内部实现
- [ ] 实现统一读取接口（get, position, limit, remaining）
- [ ] 半包场景测试验证
- [ ] 性能测试对比优化前后差异

---

## P1 - 中期优化（提升稳定性和灵活性）

### 4. 业务线程池隔离 ⚠️ 待核实

**问题描述：**
- HandlerRunnable 在 tioExecutor 中执行
- 若业务 handler() 耗时阻塞，会占用核心线程池资源
- 影响编解码效率，甚至导致系统雪崩

**优化方案：**
- 在 TioConfig 中增加独立的 businessExecutor 配置项
- 提供配置选项决定是否使用独立线程池
- 耗时业务操作自动切换到 businessExecutor

**关键代码位置：**
- `mica-net-core/src/main/java/org/tio/core/TioConfig.java`
- `mica-net-core/src/main/java/org/tio/core/task/HandlerRunnable.java`

**实施步骤：**
- [ ] 在 TioConfig 中添加 businessExecutor 字段
- [ ] 提供配置方法 setBusinessExecutor(ExecutorService)
- [ ] 修改 HandlerRunnable 判断逻辑
- [ ] 增加业务线程池监控指标
- [ ] 编写配置示例文档
- [ ] 测试不同线程池配置下的性能表现

> ⚠️ **状态待核实**：`businessExecutor` 配置项目前未在代码中实现，仅在 TODO 文档中存在。

---

### 5. DirectByteBuffer 混合策略

**问题描述：**
- 当前主要使用 HeapByteBuffer
- 大包场景下存在内核态到用户态的额外拷贝

**优化方案：**
- 对大包（> 64KB）使用 DirectByteBuffer 减少内核拷贝
- 小包仍用 HeapBuffer 避免 DirectMemory 碎片
- 在 TioConfig 中提供配置开关

**关键代码位置：**
- `mica-net-core/src/main/java/org/tio/core/TioConfig.java`
- ByteBuffer Pool 实现（配合 P0-1）

**实施步骤：**
- [ ] 在 TioConfig 添加配置项（directBufferThreshold, useDirectBuffer）
- [ ] 修改 ByteBufferPool 支持 Direct/Heap 混合分配
- [ ] 增加 DirectMemory 使用量监控
- [ ] 测试大包场景性能提升
- [ ] 编写最佳实践文档

---

### 6. 统计模块分级控制 ⚠️ 待核实

**问题描述：**
- 当前 `TioConfig.statOn` 是单一 boolean 开关，不够灵活
- 统计操作（AtomicLong.addAndGet）在极高 QPS 下仍有微量开销

**优化方案：**
- 提供分级统计机制：
  - Level 0: 关闭所有统计
  - Level 1: 仅统计连接数和错误数
  - Level 2: 增加流量统计（receivedBytes, sentBytes）
  - Level 3: 全量统计（包括耗时、队列深度等）

**关键代码位置：**
- `mica-net-core/src/main/java/org/tio/core/TioConfig.java`
- `mica-net-core/src/main/java/org/tio/core/stat/ChannelStat.java`
- `mica-net-core/src/main/java/org/tio/core/stat/GroupStat.java`

**实施步骤：**
- [ ] 设计 StatLevel 枚举（OFF, BASIC, NORMAL, DETAILED）
- [ ] 在 TioConfig 中添加 setStatLevel(StatLevel level)
- [ ] 修改各统计点增加级别判断
- [ ] 性能测试不同级别的性能差异
- [ ] 更新配置文档

> ⚠️ **状态待核实**：`TioConfig` 中目前仅有 `statOn` 单一 boolean 开关，`StatLevel` 分级机制未在代码中实现。

---

## P2 - 长期优化（微优化和扩展性）

### 7. 集合分片优化（支持百万级连接）

**问题描述：**
- TioConfig 维护多维度映射（Users, Groups, Ids, Tokens）
- 百万级连接下，多个 ConcurrentHashMap 同步维护带来压力

**优化方案：**
- 引入分段 Map 或分片策略
- 提供连接清理策略配置（idle timeout 自动清理）
- 考虑使用更紧凑的数据结构（如 Roaring Bitmap 存储 ID 集合）

**关键代码位置：**
- `mica-net-core/src/main/java/org/tio/core/TioConfig.java`
- `mica-net-core/src/main/java/org/tio/core/maintain/*`（各维度管理类）

**实施步骤：**
- [ ] 分析当前集合操作的热点路径
- [ ] 设计分片策略（按 hash 或 ID 范围分片）
- [ ] 实现分片 Map 包装类
- [ ] 增加自动清理机制（基于 idle time）
- [ ] 百万级连接压力测试
- [ ] 内存占用对比测试

---

### 8. write 分散聚集 I/O 支持

**问题描述：**
- 当前批量发送需要将多个小包合并到一个 ByteBuffer
- 合并过程仍涉及内存拷贝

**优化方案：**
- 使用 `AsynchronousSocketChannel.write(ByteBuffer[] srcs)`
- 避免多个小包合并时的内存拷贝
- 直接传递 ByteBuffer 数组给内核

**关键代码位置：**
- `mica-net-core/src/main/java/org/tio/core/tcp/TcpSendRunnable.java`

**实施步骤：**
- [ ] 修改 SendRunnable 支持 ByteBuffer[] 批量发送
- [ ] 增加配置开关（useWrite）
- [ ] 测试不同包大小下的性能表现
- [ ] 对比单 Buffer 和数组方式的性能差异

---

### 9. 同步消息机制优化 ⚠️ 文档标记失实

**问题描述：**
```java
// HandlerRunnable 同步消息处理（当前实现）
synchronized(initPacket) {
    initPacket.notify();
}
```
- Monitor 锁开销较大
- 在高并发同步调用下存在竞争

**优化方案：CompletableFuture 异步响应**

> ⚠️ **严重错误**：以下文档标记为"✅ 已完成"，但 **代码中不存在** 对应实现。
>
> 核查结果：
> - `org.tio.core.async.TioFuture` ❌ 不存在
> - `Tio.sendAsync()` / `Tio.sendAndAwait()` ❌ 不存在
> - `waitingAsyncResps` ❌ 不存在于 TioConfig
> - `HandlerRunnable.java` 中无异步 API 相关代码
> - 测试文件 `TioAsyncExample.java` ❌ 不存在
>
> **文档中的"已完成"标记与代码现状严重不符，需核实原实现是否被回滚或从未合并。**

**待实施步骤：**
- [ ] 核查原实现历史（是否曾有 CompletableFuture 方案）
- [ ] 如需重新实现，参考文档中的设计思路
- [ ] 实现 TioFuture 核心类
- [ ] 在 Tio 中添加异步 API
- [ ] 编写测试和示例

---

### 10. 慢包攻击检测优化 ✅ 已完成

**问题描述：**
- DecodeRunnable 每次解码都计算平均包长
- 虽然逻辑简单，但在极高 QPS 下仍有优化空间

**优化方案：滑动窗口算法** ✅

已实现基于滑动窗口的慢包检测器，显著降低计算开销。

**已完成的实现：**

1. ✅ **核心类 SlowPacketDetector**
   - 位置：`org.tio.core.stat.SlowPacketDetector`
   - 功能：环形缓冲区实现的滑动窗口统计
   - 特性：无锁设计，O(1) 时间复杂度

2. ✅ **TioConfig 配置支持**
   - `enableSlowPacketDetection`：检测开关（默认 true）
   - `slowPacketWindowSize`：滑动窗口大小（默认 16）
   - `slowPacketCheckInterval`：检测间隔（默认 1，每次失败都检测）
   - `maxDecodeFailCount`：最大失败次数阈值

3. ✅ **TcpDecodeRunnable 集成**
   - 在解码失败时记录数据长度到滑动窗口
   - 使用 `shouldCheck()` 降低检测频率
   - 自动计算滑动窗口内的平均值
   - 代码位置：`TcpDecodeRunnable.java:124-170`

4. ✅ **ChannelStat 扩展**
   - 延迟初始化检测器（避免无用开销）
   - 通过 `hasSlowPacketDetector()` 判断是否已启用
   - 支持自定义窗口大小和检测间隔

**优化效果：**
- ✅ 使用环形缓冲区避免重复计算
- ✅ O(1) 复杂度更新和查询
- ✅ 支持可配置的检测频率（降低 CPU 开销）
- ✅ 无锁设计适合高并发场景
- ✅ 向后兼容（可通过配置关闭）

**配置示例：**

```java
// 启用慢包检测，使用 32 个样本的滑动窗口，每 5 次失败检测一次
TioConfig tioConfig = new TioConfig();
tioConfig.setEnableSlowPacketDetection(true);
tioConfig.setSlowPacketWindowSize(32);
tioConfig.setSlowPacketCheckInterval(5);  // 降低检测频率，减少 CPU 开销
tioConfig.setMaxDecodeFailCount(10);
```

**实施状态：**
- [x] 实现滑动窗口统计算法
- [x] 添加检测频率配置
- [x] 增加检测开关（enableSlowPacketDetection）
- [x] 集成到 TcpDecodeRunnable
- [x] 延迟初始化机制
- [ ] 性能测试对比优化前后（待补充）

**Git 提交：**
- d8eaf3c: feat(stat): 添加慢包攻击检测器及滑动窗口算法优化

---

## 补充优化项

### 11. 对象复用优化

**问题描述：**
- Packet 对象频繁创建

**优化方案：**
- 为高频协议（如心跳包）提供单例复用
- 对固定格式的响应包使用对象池
- 在 TioHandler 中提供 newPacket() 接口支持自定义池化

**实施步骤：**
- [ ] 分析 Packet 创建热点
- [ ] 设计对象池接口
- [ ] 实现心跳包单例
- [ ] 提供配置选项

---

### 12. 编解码性能监控

**问题描述：**
- 缺少编解码性能的细粒度监控

**优化方案：**
- 增加编解码耗时统计
- 记录慢编解码操作（超过阈值）
- 提供性能诊断 API

**实施步骤：**
- [ ] 在 DecodeRunnable 增加耗时统计
- [ ] 在 SendRunnable 增加编码耗时统计
- [ ] 增加慢操作日志（可配置阈值）
- [ ] 提供 JMX 监控接口

---

## 性能测试计划

### 基准测试场景

1. **高并发短连接**
   - 10K QPS，每个连接发送 10 个包后断开
   - 关注：连接建立/销毁开销、集合操作性能

2. **长连接高吞吐**
   - 1K 长连接，每秒 100K 小包（< 1KB）
   - 关注：ByteBuffer 分配、GC 频率

3. **大包传输**
   - 1K 长连接，每秒 1K 大包（> 64KB）
   - 关注：DirectBuffer 效果、内存拷贝优化

4. **半包场景**
   - 模拟网络抖动，50% 包被拆分
   - 关注：CompositeByteBuffer 效果

### 性能指标

- [ ] 吞吐量（QPS/TPS）
- [ ] 延迟（P50/P95/P99）
- [ ] 内存占用（堆内存、DirectMemory）
- [ ] GC 频率和停顿时间
- [ ] CPU 使用率

---

## 实施建议

### 阶段 1（1-2 周）
- 完成 P0-1: ByteBuffer 池化机制
- 完成 P0-2: 消除 ReadCompletionHandler 拷贝

### 阶段 2（1-2 周）
- 完成 P0-3: CompositeByteBuffer 实现
- 完成 P1-4: 业务线程池隔离

### 阶段 3（2-3 周）
- 完成 P1-5: DirectByteBuffer 混合策略
- 完成 P1-6: 统计模块分级控制
- 进行全面性能测试

### 阶段 4（按需）
- 根据性能测试结果选择实施 P2 优化项

---

## 注意事项

1. **向后兼容性**
   - 所有优化必须保持 API 兼容性
   - 新功能提供配置开关，默认关闭

2. **测试覆盖**
   - 每个优化必须有对应的单元测试
   - 关键优化需要压力测试验证

3. **文档更新**
   - 更新 CLAUDE.md 中的优化说明
   - 提供配置示例和最佳实践

4. **性能回归测试**
   - 建立自动化性能测试基准
   - 每次优化后进行对比测试

---

**最后更新：** 2026-04-02（经代码核查后修正）
**状态：** 待实施
**预期总体收益：** 内存占用 ↓40-50%，GC 停顿 ↓60%+，吞吐量 ↑30-50%
