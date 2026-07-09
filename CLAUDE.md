# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

mica-net 是基于 t-io 简化而来的 Java 网络通信框架，使用 Java NIO `AsynchronousSocketChannel` 实现异步非阻塞网络通信。最低编译目标为 **Java 1.8**，默认 Maven 多模块工程（`<revision>2.0.11-SNAPSHOT</revision>`）。自 mica-net 2.0.0 起包名从 `org.tio` 迁移为 `net.dreamlu.mica.net.*`。

核心能力：TCP/UDP 通信、HTTP/HTTPS、WebSocket、SSE、`Model Context Protocol (MCP)` 服务端、PROXY protocol V1/V2、SSL/TLS（双向认证、PKCS12）、节点集群与心跳。

## 模块结构

```
mica-net-utils     基础工具（buffer、cache、hutool、json、queue、timer、thread 等）
mica-net-core      AIO 核心：AIO TCP/UDP、SSL、PROXY protocol、集群、统计
mica-net-http      基于 core 构建的 HTTP、WebSocket、SSE、HttpRouter、MCP 服务端
mica-net-http 依赖于 mica-net-core，而 mica-net-core 依赖于 mica-net-utils
```

## 常用命令

### 构建

```bash
# 全量构建（默认跳过测试，根 pom 中 <maven.test.skip>true</maven.test.skip>）
mvn clean install

# 构建并运行测试（必须显式覆盖 skip）
mvn clean install -Dmaven.test.skip=false

# 仅打包，跳过测试
mvn clean package -Dmaven.test.skip=true

# 单独构建一个模块（-am 会把依赖模块也一起构建）
mvn clean install -pl mica-net-core -am

# 发布（CI 流程是 mvn clean package -P !develop）
mvn clean package -P !develop
./deploy.sh release   # 实际部署，会调用 vfox 切到 java 8
```

### 测试

测试使用 JUnit 5 + tinylog（参考 `mica-net-core/src/test/java`）。注意根 pom 默认 `maven.test.skip=true`，所以必须显式 `-Dmaven.test.skip=false`。

```bash
# 跑全部测试
mvn test -Dmaven.test.skip=false

# 跑单个测试类
mvn -pl mica-net-core test -Dmaven.test.skip=false -Dtest=TcpClientTest

# 跑单个方法
mvn -pl mica-net-core test -Dmaven.test.skip=false -Dtest=ProxyProtocolDecoderTest#testIPV4Decode
```

### 配置仓库

`develop` profile（默认激活）走阿里云镜像。CI 用 `mvn clean package -P !develop` 关闭 develop profile 后使用 Maven Central。

## 架构核心（必须先理解）

### 三层任务队列

```
[网络I/O] → DecodeRunnable → HandlerRunnable → SendRunnable → [网络I/O]
```

- `AbstractDecodeRunnable` / `TcpDecodeRunnable`：粘包/半包解码，循环调用 `TcpHandler.decode`；包含滑动窗口慢包攻击检测
- `HandlerRunnable`：业务处理；`synSeq > 0` 时通过 `CompletableFuture` 异步响应；调用 `TcpHandler.handler`
- `AbstractSendRunnable` / `TcpSendRunnable`：自适应批量发送，scatter-write 零拷贝

读路径入口：`ReadCompletionHandler.completed` → 解码 → 业务处理；写路径由 `WriteCompletionHandler.completed` 续写。

### 关键类与职责

| 类型 | 说明 |
| ---- | ---- |
| `Tio` | 对外 API 入口：`send/bSend/close/bind/unbind/schedule` 等 |
| `TioConfig / TioServerConfig / TioClientConfig` | 全局配置（线程池、统计、心跳、SSL、backlog） |
| `ChannelContext` | 每个 TCP/UDP 连接对应一个上下文；`states` 为 `final AtomicInteger`（CAS），业务可通过 `tioConfig` 访问全局配置 |
| `ServerChannelContext / ClientChannelContext` | 扩展自 `ChannelContext`，按端区分 |
| `NetChannel / UdpChannel` | 抽象网络通道；TCP/UDP 共用抽象 |
| `TcpHandler` | TCP 三段式接口（`decode/encode/handler`），`TcpServerHandler`、`TcpClientHandler` 是分别面向 server/client 的子接口 |
| `UdpHandler` | UDP 业务接口（基于 `UdpChannel`） |
| `ReadCompletionHandler / WriteCompletionHandler` | AIO 完成处理器 |
| `Packet / EncodedPacket / IgnorePacket` | 业务包抽象；`EncodedPacket` 直接携带已编码字节 |
| `TioServer / TioClient` | 服务端/客户端启动入口 |

### ChannelContext 状态位

`ChannelContext#states` 为 `AtomicInteger`，通过位运算标识连接状态，业务可使用 `isAccepted`、`isBizStatus` 等预留位。状态变更使用 CAS 循环保证线程安全。同步包使用 `synSeq`，通过 `CompletableFuture` 实现请求-响应模式。

### 网络层默认行为

- 客户端 `close`：调用 `Tio.close`，会触发 `ReconnConf` 自动重连
- 服务端踢人：使用 `Tio.remove`（不是 close）
- 心跳机制基于 `HashedWheelTimer` 时间轮（不再单独线程）
- `useQueueDecode`、`useQueueSend` 控制是否走线程池；设为 `false` 可减少一次切换
- `writing` 标志保护 `WritePendingException`

### 高层模块要点

- **PROXY Protocol**：`server/proxy/ProxyProtocolDecoder` 同时支持 v1/v2，未携带 PROXY header 的连接不会断开（灰度友好）
- **SSL**：`core/ssl/SslConfig.forServer()/forClient()`，可注册 `SSLEngineCustomizer` 自定义协议版本与加密套件；`SslFacade` 解密使用 `slice()` 避免复制字节缓冲区
- **集群**：`server/cluster` 提供多实例 group/user 同步、广播、节点上下线通知；`lateJoinMembers` 为线程安全 Set
- **HTTP 模块**：`HttpServerStarter` + 函数式 `HttpRequestHandler`；`HttpRouter` 是基于 Trie 的轻量路由，支持路径参数 + 过滤器 + 全局异常；`HttpStream.startSse` 实现 SSE/chunked
- **WebSocket**：`WsServerStarter` + `IWsMsgHandler`（`onText/onBytes/onClose/handshake`），客户端数据必须 mask
- **MCP（mica-net-http）**：实现 `tools`、`resources`、`prompts`、`sampling`、`roots`、`completion`、`cancellation` 等协议能力，位于 `http/mcp`

## 协议：三段式编解码

`TcpHandler.decode` 返回 `null` 表示半包，框架会自动拼接 `lastByteBuffer` 等待下次数据：**不够一帧时绝不要消费 `ByteBuffer`**。

```java
public Packet decode(ByteBuffer buf, int limit, int pos, int readable, ChannelContext ctx) {
    if (readable < HEADER_LEN) return null;  // 半包
    if (readable < HEADER_LEN + bodyLen) return null;
    // 完整帧：从 buf 取数据构造 Packet
}
```

`encode` 返回 `ByteBuffer`，`SendRunnable` 会自动按需做 SSL 加密和 scatter-write 批量发送。

## 约定与风格

代码风格已经在 `.editorconfig` 中固化：**Java 文件使用 Tab 缩进**，JSON/YAML 用 2 空格，LF 行尾，UTF-8。

新文件必须包含 Apache 2.0 license header（参考 `mica-net-core/src/main/java/net/dreamlu/mica/net/core/intf/TcpHandler.java`）。

- 包名统一 `net.dreamlu.mica.net.*`，模块化由 `moditect-maven-plugin` 在 `package` 阶段注入 JPMS `module-info`
- 日志使用 SLF4J（`private static final Logger log = LoggerFactory.getLogger(...)`），必须用 `{}` 占位符
- 不使用通配符 import；新代码注释优先英文，但修改已用中文注释的区块时保持中文一致
- 字符串常量：`UPPER_SNAKE_CASE`；类/接口 `PascalCase`；方法/变量 `camelCase`
- `mica-net-utils` 下多套 JSON 适配（fastjson/fastjson2/gson/jackson/hutool-json/snack3/snack4），按需选用，业务代码默认走 `JsonUtil`

## 关键配置项（TioConfig）

| 字段 | 默认 | 说明 |
| ---- | ---- | ---- |
| `heartbeatTimeout` | `120000ms` | 心跳超时 |
| `heartbeatTimeoutStrategy` | `NO` | `NO` / `SEND_PING` / `DISCONNECT_RECONNECT` |
| `useQueueDecode` | `true` | 解码走线程池 |
| `useQueueSend` | `true` | 发送入队批量 |
| `backlog` | `50` | 服务端 accept backlog |
| `debug` | `false` | 是否输出详细日志 |

## 重要补充

- 较老代码可能残留 `org.tio.*` import，是 v2.0.0 之前的 t-io 风格，新写代码不要再使用
- `ChannelContext` 已不再保留旧 `TcpChannelContext`，TCP 相关字段全部回归 `ChannelContext` 自身（参见最近的 commit `60cb0aa`）
- 重连：`TioClientConfig.setReconnConf(new ReconnConf())`；v2.0.10 起 `ReconnConf#connectedFilter` 可自定义连接成功断言
- 不要提交 `.idea/`、`.codegraph/`、`.claude/`、`.workbuddy/`、`target/`、`.flattened-pom.xml`（见 `.gitignore`）
- 用户级使用文档见 `docs/useage/{tcp,udp,http,websocket}.md`；变更记录见 `CHANGELOG.md`
- 任何新的源码改动请同步更新 `CHANGELOG.md`（按 `feat|fix|refactor|perf|chore(scope): 简述` 格式）

## AI 代理协作约定

1. 改代码前先 `grep` 或 `codegraph_explore` 看清调用链，避免破坏 AIO 写路径的状态机
2. 修改公共 API（`Tio`、`TcpHandler`、`HttpRouter` 等）时同步更新 `docs/useage/` 中对应文档
3. 测试默认被跳过，提交前请跑 `mvn test -Dmaven.test.skip=false -pl <module>` 验证
4. 不要忽略 `mvn clean package` 的报错；注意 `.flattened-pom.xml` 在 .gitignore 中，每次构建会重新生成
