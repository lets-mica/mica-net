# mica-net 网络编程
[![Mica net Maven release](https://img.shields.io/nexus/r/https/oss.sonatype.org/net.dreamlu/mica-net-core.svg?style=flat-square)](https://mvnrepository.com/artifact/net.dreamlu/mica-net-core)
[![Mica net Maven snapshots](https://img.shields.io/nexus/s/https/oss.sonatype.org/net.dreamlu/mica-net-core.svg?style=flat-square)](https://oss.sonatype.org/content/repositories/snapshots/net/dreamlu/mica-net-core/)

## 注意（开发细节）

- 客户端主动断开用 close(Tio.close)，服务端主动断开用（Tio.remove）。

## TCP 相关知识

- [大小端处理](docs/tcp/大小端.md)
- [无符号处理](docs/tcp/无符号.md)

## 声明

该项目基于 t-io（Apache License v2）简化而来，并且完全遵循 **Apache License v2** 协议。

## 变更内容

1. 使用 java8 作为最低编译版本。
2. 去除了一些使用不到的模块和代码。
3. 使用并发集合替换锁，优化代码降低内存使用量。
4. stat 的 AtomicInteger 替换成 LongAdder。
5. `ChannelContext` 采用二进制位标识状态位，减少内存占用。并预留 `isAccepted`、`setAccepted` 和 `isBizStatus`、`setBizStatus` 给业务使用。
6. 添加 mica 中的 HexUtils、DigestUtils、ExceptionUtils。
7. mica-mqtt 部分工具包下沉。
8. 去除 ips 和 ip 黑名单, 不再依赖 caffeine 2.9.3。
9. 不依赖 fastjson。
10. 优化 ssl，支持客户端和服务端支持双向认证，客户端不校验域名。
11. 代码优化，更加符合规范。
