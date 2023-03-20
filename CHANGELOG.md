# 变更记录

## 发行版本

### v0.0.9 - 2023-03-26
- :fire: 优化 `ByteBufferUtil` 工具类，只 `dump` 有效的 `ByteBuffer` 数据和优化 `toString` 方法。
- :sparkles: `HttpRequestHandler#clearStaticResCache` 方法改为 default 方法。
- :sparkles: 支持空 `websocket` 包
- :sparkles: 完善 `HexUtils` 工具类。
- :sparkles: 添加 JsonUtil，支持 `Jackson`, `Fastjson1`, `Fastjson2`, `Gson`, `hutool-json`。
- :sparkles: copy 并采用时间轮优化 `TimedCache`，用于处理 mqtt 保留 session。
- :sparkles: ChannelContext 规范化，`userId` 改为 `private`。 
- :arrow_up: 依赖升级

### v0.0.8 - 2023-03-05
- :sparkles: IPropSupport 添加 `computeIfAbsent` 方法。
- :sparkles: 服务端 ssl 添加认证模式
- :sparkles: 优化首次重连的日志
- :sparkles: 优化 javadoc，任未优化完~

### v0.0.7 - 2023-02-21

- :sparkles: 版本号改为读取 jar 信息，减少硬编码。
- :sparkles: 使用 java8 default 接口方法简化代码，删除 DefaultTioListener 接口。
- :sparkles: 合并 mica-mqtt CollUtil 工具类。

### v0.0.6 - 2023-02-02

- :sparkles: 优化处理 header 长度异常。
- :sparkles: 代码优化。

### v0.0.5 - 2022-09-15

- :sparkles: 分页获取客户端支持过滤。
- :sparkles: 调整默认的线程数
- :sparkles: 代码简化和优化，减少 cpu 占用。

### v0.0.4 - 2022-09-15

- :sparkles: 优化日志，调整日志级别。
- :sparkles: 完善和优化 ssl，提升性能。
- :sparkles: 添加和使用 SystemClock，t-io 默认的 SystemTimerClock 误差很大。
- :sparkles: 添加 t-io 示例
- :bug: 修复 t-io client 重连 bug。
- :bug: 修复 t-io server 部分情况下进入异常递归，导致服务退出。

### v0.0.3 - 2022-07-18

- :sparkles: udp 代码规范化。
- :sparkles: 将 mica-mqtt 通用工具移到 mica-net-utils。

### v0.0.2 - 2022-06-18

- 使用并发集合替代锁。
- 指标替换 `AtomicLong` 为 `LongAdder`。
- 减少一些没必要的内存占用，大幅度降低启动内存。

### v0.0.1 - 2022-06-12

- 基于 t-io 3.8.1.v20220401-RELEASE 修改。
- 使用 java8 作为最低编译版本。
- 去除了一些使用不到的模块和代码。
- 添加 mica 中的 HexUtils、DigestUtils、ExceptionUtils。
- 切换到 cache2k、不依赖 fastjson。
- 代码优化和规范，减少内存占用和提高性能。
- 解码失败次数可配置化，方便大包解析。