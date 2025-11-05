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
│  提供所有对外API：send、close、bind、unbind等操作                │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    ChannelContext (连接上下文)                │
│  • 每个TCP连接对应一个ChannelContext                           │
│  • 维护连接状态、统计信息、绑定关系                               │
│  • 包含3个核心Runnable：DecodeRunnable、HandlerRunnable、      │
│    SendRunnable                                             │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────┬──────────────────┬───────────────────────┐
│  DecodeRunnable  │ HandlerRunnable  │   SendRunnable        │
│  (解码任务)       │  (业务处理任务)   │   (发送任务)             │
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
   ├─ SSL? → SSL解密
   └─ 添加到解码队列
   ↓
3. DecodeRunnable.decode()
   ├─ 处理lastByteBuffer（上次剩余数据）
   ├─ 循环解码：TioHandler.decode()
   ├─ 检测慢包攻击（连续解码失败检测）
   ├─ 解码成功 → 放入HandlerRunnable
   └─ 解码失败 → 保存lastByteBuffer等待更多数据
   ↓
4. HandlerRunnable.handler()
   ├─ 同步消息处理（synSeq机制）
   ├─ 业务处理：TioHandler.handler()
   └─ 统计处理时长
```

------

## 📤 发送处理核心逻辑

### 发送数据流程

```
1. Tio.send(channelContext, packet)
   ├─ 检查连接状态
   ├─ PacketConverter转换（可选）
   ├─ useQueueSend? 
   │   ├─ true → 添加到队列
   │   └─ false → 直接发送
   └─ 触发SendRunnable.execute()
   ↓
2. SendRunnable.runTask()
   ├─ 单包？→ 直接发送
   └─ 多包？→ 批量合并发送
       ├─ 自适应批量大小（根据队列积压）
       ├─ 编码：TioHandler.encode()
       ├─ SSL? → SSL加密
       ├─ 合并多个ByteBuffer
       └─ 调用sendByteBuffer()
   ↓
3. sendByteBuffer()
   ├─ 设置writing标记（防止WritePendingException）
   └─ AsynchronousSocketChannel.write()
   ↓
4. WriteCompletionHandler.completed()
   ├─ hasRemaining? → 继续写入
   ├─ 统计发送字节数
   ├─ 回调：channelContext.processAfterSent()
   └─ onWriteCompleted()
       ├─ 清除writing标记
       └─ 触发下一批发送
```

------

## 注意（开发细节）

- 客户端主动断开用 close(Tio.close)，服务端主动断开用（Tio.remove）。

## TCP 相关知识

- [大小端处理](docs/tcp/大小端.md)
- [无符号处理](docs/tcp/无符号.md)

## 声明

该项目基于 t-io（Apache License v2）简化而来，并且完全遵循 **Apache License v2** 协议。

## 变更内容

- 使用 java8 作为最低编译版本。 
- 去除了一些使用不到的模块和代码。 
- 使用并发集合替换锁，优化代码降低内存使用量。 
- stat 的 AtomicInteger 替换成 LongAdder。
- `ChannelContext` 采用二进制位标识状态位，减少内存占用。并预留 `isAccepted`、`setAccepted` 和 `isBizStatus`、`setBizStatus` 给业务使用。 
- 添加 mica 中的 HexUtils、DigestUtils、ExceptionUtils。 
- mica-mqtt 部分工具包下沉。
- 支持 Tcp Proxy 代理协议 v1 版，支持 nginx、elb 转发原始IP。 
- 去除 ips 和 ip 黑名单, 不再依赖 caffeine 2.9.3。 
- 不强制依赖 fastjson，支持多种 json 工具（jackson、fastjson、fastjson2、gson、hutool-json、snack3）。
- 优化 ssl，支持客户端和服务端支持双向认证，客户端不校验域名。 
- 代码优化，更加符合规范。