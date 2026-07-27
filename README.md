# mica-net Java 网络框架
[![Mica net Maven release](https://img.shields.io/maven-central/v/net.dreamlu/mica-net-core.svg?style=flat-square)](https://central.sonatype.com/artifact/net.dreamlu/mica-net-core/versions)
![Mica Maven SNAPSHOT](https://img.shields.io/maven-metadata/v?metadataUrl=https://central.sonatype.com/repository/maven-snapshots/net/dreamlu/mica-net-core/maven-metadata.xml)

## 📋 项目概述

mica-net 是 mica-mqtt 的底层网络框架，基于 Java AIO 的轻量、高性能非阻塞网络通信框架，TCP 使用 Java AIO 的 `AsynchronousSocketChannel`，UDP 使用 Java NIO 的 `DatagramChannel` 实现非阻塞网络通信。

核心能力包括：

- **TCP/UDP 网络通信**：TCP 基于 AIO `AsynchronousSocketChannel`，UDP 基于 NIO `DatagramChannel`
- **HTTP/HTTPS 与 WebSocket**：内置编解码器，支持 SSE、Stream、Router 等
- **MCP（Model Context Protocol）服务端**：完整实现 `tools`、`resources`、`prompts`、`sampling` 等协议能力
- **TCP 代理协议**：支持 PROXY protocol V1/V2，可解析 nginx、ELB 转发的原始 IP
- **SSL/TLS**：支持双向认证、PKCS12 证书，可自定义协议版本与加密套件
- **集群与节点管理**：内置集群同步、节点选择、心跳与重连

[✨✨✨推广：**BladeX 物联网平台**✨✨✨iot.bladex.cn](https://iot.bladex.cn?from=mica-mqtt)

------

## 🏗️ 核心设计架构

### 1. 核心组件设计

```
┌─────────────────────────────────────────────────────────────┐
│                         Tio (API层)                          │
│  提供所有对外API：send、close、bind、unbind等操作              │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│          NetChannel / ChannelContext / UdpChannel           │
│  • NetChannel：抽象网络通道接口（send、close）                │
│  • ChannelContext：TCP 连接上下文                            │
│  • UdpChannel：UDP 通道抽象                                  │
│  • TCP 上下文维护连接状态、统计信息、绑定关系及核心任务          │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                 TcpHandler / UdpHandler                     │
│  TcpHandler：TCP 业务处理接口（增强类型安全，泛型化）         │
│  UdpHandler：UDP 业务处理接口（基于 UdpChannel）             │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────┬──────────────────┬───────────────────────┐
│TcpDecodeRunnable │ HandlerRunnable  │  TcpSendRunnable      │
│  (解码任务)       │  (业务处理任务)   │   (发送任务)           │
└──────────────────┴──────────────────┴───────────────────────┘
```



### 2. 三层任务队列架构

```
[网络I/O] → [解码队列] → [处理队列] → [发送队列] → [网络I/O]
    ↓            ↓            ↓            ↓
ReadCompletionHandler  TcpDecodeRunnable  HandlerRunnable  TcpSendRunnable
```


### 3. 核心类职责

- TioConfig: 全局配置管理（线程池、统计、心跳、SSL等）

- NetChannel: 抽象网络通道接口（send、close），TCP/UDP 统一抽象

- ChannelContext: 连接上下文（状态、队列、统计、绑定关系），实现 NetChannel

- TcpHandler / UdpHandler: TCP/UDP 业务处理接口，强类型泛型化

- ReadCompletionHandler: 异步读完成处理器

- WriteCompletionHandler: 异步写完成处理器

- TcpDecodeRunnable: TCP 解码任务（粘包/半包处理）

- HandlerRunnable: 业务处理任务

- TcpSendRunnable: TCP 发送任务（批量处理、SSL 加密）

------

## 🔄 接收处理核心逻辑

### 接收数据流程

```
1. AsynchronousSocketChannel.read()
   ↓ [异步读取]
2. ReadCompletionHandler.completed()
   ├─ 统计接收字节数、更新时间戳
   ├─ SSL? → SslHandler.decrypt() 解密后直接触发解码
   └─ 非SSL? → useQueueDecode? 添加到队列 : 直接解码
   ↓
3. TcpDecodeRunnable.decode()
   ├─ 流式拼接：lastByteBuffer + 本次数据
   ├─ 循环解包：while(true) 调用 TcpHandler.decode()
   ├─ 检测慢包攻击（滑动窗口算法）
   ├─ 解码成功 → onDecodeSuccess() → HandlerRunnable
   └─ 数据不够 → 保存 lastByteBuffer 等待更多数据
   ↓
4. HandlerRunnable.handler()
   ├─ synSeq > 0? → CompletableFuture 异步响应
   ├─ synSeq == 0 → TcpHandler.handler() 业务处理
   └─ 统计处理时长、更新 ChannelStat
```

------

## 📤 发送处理核心逻辑

### 发送数据流程

```
1. Tio.send(channelContext, packet)
   ├─ 检查连接状态
   ├─ PacketConverter 转换（可选）
   ├─ useQueueSend?
   │   ├─ true → 添加到队列，触发 TcpSendRunnable
   │   └─ false → 直接发送
   └─ 触发 TcpSendRunnable.execute()
   ↓
2. TcpSendRunnable.runTask()
   ├─ writing.get()? → 有写操作进行中，直接返回
   ├─ 单包 → sendPacket() 编码 + SSL + sendByteBuffer()
   └─ 多包 → batchEncode() 批量编码
       ├─ 自适应批量大小（根据队列积压）
       ├─ TcpHandler.encode() 编码
       ├─ SSL 加密（encryptBatchIfNeeded）
       └─ sendByteBuffers() gathering write，避免批量缓冲区合并复制
   ↓
3. sendByteBuffer() / sendByteBuffers()
   ├─ 设置 writing.set(true) 防 WritePendingException
   └─ 统一 gathering write：AsynchronousSocketChannel.write(ByteBuffer[])
   ↓
4. WriteCompletionHandler.completed()
   ├─ hasRemaining? → gathering write 续写（利用 offset+length 参数）
   ├─ 所有 buffer 发送完毕 → handle()
   │   ├─ signal condition 唤醒等待线程
   │   ├─ 统计发送字节数
   │   ├─ processAfterSent() 回调
   │   └─ 失败时关闭连接
   └─ onWriteCompleted()
       ├─ 清除 writing.set(false)
       └─ 触发下一批发送
```

------

## 🔊 注意（开发细节）

- `Tio.close` 关闭连接时可保留客户端重连等维护逻辑，适用于客户端；
- `Tio.remove` 关闭连接后不再进行重连等维护，适用于服务端。

## 💡 使用文档（useage）

- [TCP 使用文档](docs/useage/tcp.md)
- [UDP 使用文档](docs/useage/udp.md)
- [HTTP 使用文档](docs/useage/http.md)
- [WebSocket 使用文档](docs/useage/websocket.md)
- [使用文档索引](docs/useage/README.md)

## 💡 TCP 相关知识

- [大小端处理](docs/tcp/大小端.md)
- [无符号处理](docs/tcp/无符号.md)

## 📄 声明

该项目遵循 **Apache License v2** 协议开源。

## 📝 变更内容

### 基础改造

- 使用 **Java 8** 作为最低编译版本
- mica-net 2.0.0 开始调整了包名，从 `org.tio` 迁移到 `net.dreamlu.mica.net`
- 去除了一些使用不到的模块和代码，代码更精简
- **不强制依赖 fastjson**，支持多种 JSON 工具（Jackson2、Jackson3、Fastjson、Fastjson2、Gson、Hutool-json、Snack3、Snack4）
- 添加 mica 中的 **HexUtils、DigestUtils、ExceptionUtils** 等工具类

### 内存优化

- **ChannelContext** 采用二进制位标识状态位，减少内存占用，预留 `isAccepted`、`isBizStatus` 给业务使用
- **Packet** 使用位域压缩技术，将 boolean 标志合并到 byte 中
- 组级统计使用 **LongAdder** 降低高并发下的统计竞争
- 使用**并发集合**替换锁，降低锁竞争
- **TioConfig** 字段排序优化，减少内存 padding 提升缓存命中
- **ChannelStat** 按 JVM 对齐原则重排字段，引用类型集中放置
- **Node** 类调整字段顺序，每个对象节省 4 字节

### 性能优化

- **无锁异步写入**：移除 ReentrantLock，改用无锁异步写入逻辑
- **gathering write 批量发送**：避免批量缓冲区合并复制，减少内存分配
- **SSL 解密优化**：使用 slice() 替代字节缓冲区复制，降低内存开销
- **自适应批量发送**：动态调整批量发送大小，适应高负载场景
- **滑动窗口慢包检测**：实现滑动窗口算法检测慢包攻击，降低检测开销
- **队列监控**：心跳任务中增加解码/处理/发送队列大小统计

### 网络与协议

- **UDP 简化重构**：UDP 统一为 `UdpChannel` 抽象，简化发送/关闭任务链路
- **TCP Proxy Protocol v1和v2**：支持 nginx、ELB 转发原始 IP
- **SSL 双向认证**：支持客户端和服务端双向认证，客户端可跳过域名校验
- **PKCS12 证书支持**：SSL 支持 PKCS12 证书格式
- **backlog 配置**：服务端添加 backlog 配置项

### 功能特性

- **MCP（Model Context Protocol）服务端**：内置 Tools、Resources、Prompts、Sampling、Roots 等协议能力
- **HttpRouter**：mica-net-http 提供轻量级路由，方便使用
- **SSE（Server-Sent Events）**：支持 HTTP Server-Sent Events
- **时间轮心跳**：服务端心跳改为时间轮，减少线程数
- **心跳超时策略**：支持 HeartbeatTimeoutStrategy，支持发送 ping 或断开等待重连
- **模块化支持**：添加 `module-info.java`，支持 Java Platform Module System
- **SSLEngineCustomizer**：用于配置 TLS 协议版本和加密套件

### 开发体验

- **简化 TioConfig**：不继承 `MapPropSupport`，使用更简洁
- **FileQueue**：内置文件队列，支持 GraalVM
- **JacksonJsonAdapter**：调整默认配置，优化序列化/反序列化行为
