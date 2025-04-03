# 变更记录

## 发行版本

### v1.1.5 - 2025-04-12

-  :sparkles: TioServer 更好的支持 Android

### v1.1.4 - 2025-03-14

- :sparkles: 客户端支持 HeartbeatTimeoutStrategy

### v1.1.2 - 2025-03-12

- :sparkles: 客户端支持 HeartbeatTimeoutStrategy，心跳超时时，支持发送ping或者断开等待重连。
- :bug: 修复 ReadCompletionHandler 读取时有可能出现异常

### v1.1.1 - 2025-03-10

- :sparkles: 修复还原 mica-net-http 中部分代码，避免 mica-mqtt 不兼容。

### v1.1.0 - 2025-03-07

- :sparkles: 调整 cache 同步最新 hutool
- :sparkles: 客户端连接时指定端口，重连时仍然指定端口。
- :sparkles: 简化，删除一些没有用到的代码
- :sparkles: mica-mqtt 中的 PayloadEncode 下沉到 mica-net
- :sparkles: mica-mqtt 中的 压缩接口下沉到 mica-net

### v1.0.13 - 2025-01-24

- :sparkles: TioServer 优化打印，更好的支持 Android

### v1.0.12 - 2025-01-23

- :sparkles: tcp server 如果非 debug 不开启版本信息等打印，方便支持 Android（Android 下没有 ManagementFactory）

### v1.0.11 - 2025-01-20
- :sparkles: tcp 代理协议支持下沉到 mica-net（最终版）

### v1.0.8 - 2024-11-15
- :sparkles: ssl 支持 `PKCS12` 证书。

### v1.0.7 - 2024-09-12
- :sparkles: `ThreadUtils` 不再将线程池作为静态变量，更好的支持 `stop`。
- :sparkles: 调整 client 默认线程数配置
- :sparkles: 优化 `TioCallerRunsPolicy` 异常日志打印

### v1.0.6 - 2024-08-31
- :sparkles: 支持模块化，添加 `module-info.java`
- :sparkles: 新增 `SSLEngineCustomizer`，用于配置 tls 协议版本和加密套件。

### v1.0.5 - 2024-08-01
- :sparkles: 支持 noear snack3 json

### v1.0.4 - 2024-07-22
- :sparkles: TioServer 添加 schedule 系列方法
- :sparkles: 时间轮和其他代码优化。
- :memo: 优化完成全部的 javadoc 告警。
- :bug: tcp ssl 客户端如果是服务端重启，需要重新生成 SSLContext 对象

### v1.0.2 - 2024-06-24
- :sparkles: 优化心跳日志
- :sparkles: 优化心跳保活策略 `HeartbeatMode`，添加 `ANY`，区分 `ANY` 和 `ALL`，并默认为 `ANY`。

### v1.0.1 - 2024-06-24
- :sparkles: 服务端心跳改为时间轮
- :sparkles: 服务端添加 `heartbeatBackoff`，默认为：1.0F（保持跟老版本兼容）
- :sparkles: 添加心跳检测模式 `HeartbeatMode`，支持 LAST_REQ、LAST_RESP 和 ALL，默认：ALL（保持跟老版本兼容）
- :sparkles: 简化 `TioConfig` 不继承 `MapPropSupport`
- :sparkles: 完善 `FileQueue` 准备使用
- :sparkles: 添加 `NodeSelector`，为客户端集群做准备
- :sparkles: 调试集群，优化集群 data 消息支持的消息长度。
- :sparkles: 升级 `github action`，优化 maven 配置
- :sparkles: 添加 `renovate bot` 方便升级依赖和插件

### v1.0.0 - 2024-05-25
- :sparkles: 只在发送成功时更新发送时间
- :sparkles: 完善 IPropSupport，方便使用，添加  getAndRemove
- :sparkles: 代码优化和依赖升级

### v0.1.12 - 2024-05-11
- :sparkles: mica-net-http 优化 cookie，调用时再解析
- :sparkles: mica-net-core 添加 stat vo 方便获取客户端和服务端状态

### v0.1.11 - 2024-04-13
- :sparkles: 服务端添加队列数据统计。
- :sparkles: 客户端重连统计日志受 debug 控制。

### v0.1.10 - 2024-04-09
- :sparkles: 优化和完善 FastByteBuffer 和 ByteBufferUtil
- :sparkles: ChannelContext 添加 send、bSend 和 getDecodeQueueSize（注意：默认对线程解码会返回-1）、getHandlerQueueSize、getSendQueueSize
- :sparkles: 完善 HexUtils 工具类
- :sparkles: 文件队列添加 graalvm 支持
- :sparkles: 升级 maven-jar-plugin

### v0.1.9 - 2024-01-15
- :sparkles: 服务端和客户端 `stop` 代码优化
- :sparkles: 完善 ByteBufferUtil，添加 `readMedium`、`readMediumLE`、`readMediumBE`
- :sparkles: 优化 DateUtil
- :sparkles: 生成的 jar 包含 JarIndex
- :arrow_up: 依赖升级

### v0.1.8 - 2023-12-25
- :sparkles: 同步更新 t-io ssl，缓解 wss 频繁刷新时的问题，优化 ssl 代码。
- :sparkles: `ChannelContext` 采用二进制位标识状态位，预留 isAccepted、setAccepted 和 isBizStatus、setBizStatus 给业务。
- :sparkles: 代码优化，缩短生成的字符串，优化为36进制字符串。
- :sparkles: 添加 -parameters 编译参数。

### v0.1.7 - 2023-11-26
- :sparkles: PROXY protocol 支持优化，开启式支持不带 PROXY protocol 报文的连接。
- :sparkles: 完善 `ByteBufferUtil` 工具，添加 `readUnsignedShort`、`readUnsignedMedium`、`readUnsignedInt` 等方法。

### v0.1.6 - 2023-10-05
- :sparkles: Threads 移动并改名为 ThreadUtils。
- :sparkles: ThreadUtils biz 线程池支持虚拟线程。

### v0.1.5 - 2023-09-02
- :sparkles: 优化版本号写入到 `Version.java`，达成 fatjar 时读取不到版本。
- :sparkles: http gzip 完善判断，对于不支持 gzip 的，不压缩。
- :arrow_up: 依赖升级

### v0.1.4 - 2023-07-16
- :sparkles: 文件队列 java8 使用 `DirectByteBuffer` 上的 `cleaner`
- :sparkles: `leon` 勇哥的文件队列合并到 util
- :sparkles: 停止时清理配置对象
- :sparkles: 集群功能合并进 core，待集成。
- :bug: 修复服务端心跳，第一次 `sleep` 太久。

### v0.1.3 - 2023-06-09
- :sparkles: 优化心跳日志，受 debug 控制
- :sparkles: 优化 `HttpConfig` 默认参数，最大请求头默认改为 **8K**。
- :sparkles: 完善 `ByteBufferUtil` 对**大小端**、**无符号**的处理，方便使用。
- :memo: docs 目录添加文档，方便上手 mica-net **tcp** 开发。

### v0.1.2 - 2023-05-27
- :sparkles: 添加 `ProxyProtocolDecoder` 支持 nginx 开启 tcp proxy_protocol on; 时转发源 ip 信息。
- :sparkles: 精简 `Packet`，减少内存占用
- :sparkles: 回滚 `ChannelStat` 使用 `AtomicLong` 更加合适，内存占用更少。
- :sparkles: 合并 `WriteBuffer` 到 `FastByteBuffer`。
- :sparkles: 添加 `mica-net-cluster` 集群模块。
- :sparkles: 优化 `EncodedPacket`，不再需要手动 `encode` 转换。
- :sparkles: `TioServerListener#onHeartbeatTimeout` 方法参数 `interval` 类型由 `Long` 改为 `long`。
- :sparkles: 添加 `FIFOCache`、`LFUCache`、`LRUCache`。
- :sparkles: 合并工具类 `ByteBufferUtils` 方法到 `ByteBufferUtil` 并完善 `ByteBufferUtil`。
- :sparkles: Node 删除没用用到的 `ssl` 属性。

### v0.1.1 - 2023-05-09
- :sparkles: 下沉 mica-mqttx 中的 `schedule` 方法
- :sparkles: 添加 mica-mqttx 中的 `WriteBuffer`
- :sparkles: 合并 mica-mqttx `ThreadUtils` 到 `Threads`
- :sparkles: 完善 `ByteBufferUtil`
- :sparkles: 客户端心跳、重连改为时间轮，减少线程数
- :sparkles: 完善 `FastByteBuffer`
- :sparkles: 客户端心跳、重连改为时间轮，减少线程数。
- :sparkles: 添加 `IgnorePacket` 包，支持忽略 `handler` 处理。

### v0.1.0 - 2023-04-23
- :sparkles: 完善 ByteBufferUtil
- :sparkles: 去掉网址打印
- :bug: 修复 hexDump 会消耗 ByteBuffer 内容

### v0.0.9 - 2023-03-23
- :fire: 优化 `ByteBufferUtil` 工具类，只 `dump` 有效的 `ByteBuffer` 数据和优化 `toString` 方法。
- :sparkles: `HttpRequestHandler#clearStaticResCache` 方法改为 default 方法。
- :sparkles: 支持空 `websocket` 包
- :sparkles: 完善 `HexUtils` 工具类。
- :sparkles: 添加 JsonUtil，支持 `Jackson`, `Fastjson1`, `Fastjson2`, `Gson`, `hutool-json`。
- :sparkles: copy 并采用时间轮优化 `TimedCache`，用于处理 mqtt 保留 session。
- :sparkles: ChannelContext 规范化，`userId` 改为 `private`，请使用 `context.getUserId()`。 
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