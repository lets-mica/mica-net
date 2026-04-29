# mica-net 网络编程
[![Mica net Maven release](https://img.shields.io/maven-central/v/net.dreamlu/mica-net-core.svg?style=flat-square)](https://central.sonatype.com/artifact/net.dreamlu/mica-net-core/versions)
![Mica Maven SNAPSHOT](https://img.shields.io/maven-metadata/v?metadataUrl=https://central.sonatype.com/repository/maven-snapshots/net/dreamlu/mica-net-core/maven-metadata.xml)

## 📋 项目概述

mica-net 是基于 t-io 简化而来的高性能 Java 网络通信框架，使用 Java NIO 的 AsynchronousSocketChannel 实现异步非阻塞网络通信。

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
│                    ChannelContext (连接上下文)               │
│  • 每个TCP连接对应一个ChannelContext                          │
│  • 维护连接状态、统计信息、绑定关系                            │
│  • 包含3个核心Runnable：DecodeRunnable、HandlerRunnable、     │
│    SendRunnable                                             │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────┬──────────────────┬───────────────────────┐
│  DecodeRunnable  │ HandlerRunnable  │   SendRunnable        │
│  (解码任务)       │  (业务处理任务)   │   (发送任务)           │
└──────────────────┴──────────────────┴───────────────────────┘
```



### 2. 三层任务队列架构

```
[网络I/O] → [解码队列] → [处理队列] → [发送队列] → [网络I/O]
    ↓            ↓            ↓            ↓
ReadHandler  DecodeRunnable  HandlerRunnable  SendRunnable
```


### 3. 核心类职责

- TioConfig: 全局配置管理（线程池、统计、心跳、SSL等）

- ChannelContext: 连接上下文（状态、队列、统计、绑定关系）

- ReadCompletionHandler: 异步读完成处理器

- WriteCompletionHandler: 异步写完成处理器

- DecodeRunnable: 解码任务（粘包/半包处理）

- HandlerRunnable: 业务处理任务

- SendRunnable: 发送任务（批量合并、SSL加密）

------

## 🔄 接收处理核心逻辑

### 接收数据流程

```
1. AsynchronousSocketChannel.read()
   ↓ [异步读取]
2. ReadCompletionHandler.completed()
   ├─ 统计接收字节数、更新时间戳
   ├─ SSL? → SslFacade.decrypt() 解密后直接触发解码
   └─ 非SSL? → useQueueDecode? 添加到队列 : 直接解码
   ↓
3. TcpDecodeRunnable.decode()
   ├─ 流式拼接：lastByteBuffer + 本次数据
   ├─ 循环解包：while(true) 调用 TioHandler.decode()
   ├─ 检测慢包攻击（滑动窗口算法）
   ├─ 解码成功 → onDecodeSuccess() → HandlerRunnable
   └─ 数据不够 → 保存 lastByteBuffer 等待更多数据
   ↓
4. HandlerRunnable.handler()
   ├─ synSeq > 0? → CompletableFuture 异步响应
   ├─ synSeq == 0 → TioHandler.handler() 业务处理
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
   │   ├─ true → 添加到队列，触发 SendRunnable
   │   └─ false → 直接发送
   └─ 触发 SendRunnable.execute()
   ↓
2. TcpSendRunnable.runTask()
   ├─ writing.get()? → 有写操作进行中，直接返回
   ├─ 单包 → sendPacket() 编码 + SSL + sendByteBuffer()
   └─ 多包 → batchEncode() 批量编码
       ├─ 自适应批量大小（根据队列积压）
       ├─ TioHandler.encode() 编码
       ├─ SSL 加密（encryptBatchIfNeeded）
       └─ sendByteBuffers() gather-write 零拷贝
   ↓
3. sendByteBuffer() / sendByteBuffers()
   ├─ 设置 writing.set(true) 防 WritePendingException
   └─ 统一 scatter-write：AsynchronousSocketChannel.write(ByteBuffer[])
   ↓
4. WriteCompletionHandler.completed()
   ├─ hasRemaining? → scatter-write 续写（利用 offset+length 参数）
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

- 客户端主动断开用 close(Tio.close)，服务端主动断开用（Tio.remove）。

## 💡 TCP 相关知识

- [大小端处理](docs/tcp/大小端.md)
- [无符号处理](docs/tcp/无符号.md)

## 📄 声明

该项目基于 t-io（Apache License v2）简化而来，并且完全遵循 **Apache License v2** 协议。

## 📝 变更内容

### 基础改造

- 使用 **Java 8** 作为最低编译版本
- 基于 **t-io 3.8.1.v20220401-RELEASE** 简化而来
- mica-net 2.0.0 开始调整了包名，从 `org.tio` 迁移到 `net.dreamlu.mica.net` 避免跟原版 t-io 包冲突。
- 去除了一些使用不到的模块和代码，代码更精简
- **不强制依赖 fastjson**，支持多种 JSON 工具（Jackson2、Jackson3、Fastjson、Fastjson2、Gson、Hutool-json、Snack3、Snack4）
- 添加 mica 中的 **HexUtils、DigestUtils、ExceptionUtils** 等工具类

### 内存优化

- **ChannelContext** 采用二进制位标识状态位，减少内存占用，预留 `isAccepted`、`isBizStatus` 给业务使用
- **Packet** 使用位域压缩技术，将 boolean 标志合并到 byte 中
- **LongAdder** 替换 AtomicInteger，提升统计性能
- 使用**并发集合**替换锁，降低锁竞争
- **TioConfig** 字段排序优化，减少内存 padding 提升缓存命中
- **ChannelStat** 按 JVM 对齐原则重排字段，引用类型集中放置
- **Node** 类调整字段顺序，每个对象节省 4 字节

### 性能优化

- **无锁异步写入**：移除 ReentrantLock，改用无锁异步写入逻辑
- **scatter-write 零拷贝**：优化异步写入为零拷贝批量发送，减少内存分配
- **SSL 解密优化**：使用 slice() 替代字节缓冲区复制，降低内存开销
- **自适应批量发送**：动态调整批量发送大小，适应高负载场景
- **滑动窗口慢包检测**：实现滑动窗口算法检测慢包攻击，降低检测开销
- **队列监控**：心跳任务中增加解码/处理/发送队列大小统计

### 网络与协议

- **UDP 重构**：UDP 重构为 NIO UDP 形式，统一 TCP、UDP 编解码和处理
- **TCP Proxy Protocol v1和v2**：支持 nginx、ELB 转发原始 IP
- **SSL 双向认证**：支持客户端和服务端双向认证，客户端可跳过域名校验
- **PKCS12 证书支持**：SSL 支持 PKCS12 证书格式
- **backlog 配置**：服务端添加 backlog 配置项

### 功能特性

- **SSE（Server-Sent Events）**：支持 HTTP Server-Sent Events
- **时间轮心跳**：服务端心跳改为时间轮，减少线程数
- **心跳超时策略**：支持 HeartbeatTimeoutStrategy，支持发送 ping 或断开等待重连
- **虚拟线程支持**：biz 线程池支持虚拟线程
- **模块化支持**：添加 `module-info.java`，支持 Java Platform Module System
- **SSLEngineCustomizer**：用于配置 TLS 协议版本和加密套件

### 开发体验

- **简化 TioConfig**：不继承 `MapPropSupport`，使用更简洁
- **FileQueue**：内置文件队列，支持 GraalVM
- **NodeSelector**：为客户端集群做准备
- **JacksonJsonAdapter**：调整默认配置，优化序列化/反序列化行为