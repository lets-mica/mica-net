# mica-net 网络编程
[![Mica net Maven release](https://img.shields.io/maven-central/v/net.dreamlu/mica-net-core.svg?style=flat-square)](https://central.sonatype.com/artifact/net.dreamlu/mica-net-core/versions)

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