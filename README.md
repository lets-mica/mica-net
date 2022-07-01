# mica-net 网络编程

## 声明
该项目基于 t-io（Apache License v2）简化而来，并且完全遵循 **Apache License v2** 协议。

## 变更内容
1. 使用 java8 作为最低编译版本。
2. 去除了一些使用不到的模块和代码。
3. 使用并发集合替换锁。
4. stat 的 AtomicInteger 替换成 LongAdder。
5. 添加 mica 中的 HexUtils、DigestUtils、ExceptionUtils。
6. 降级到 caffeine 2.9.3。
7. 不依赖 fastjson。
8. 代码优化，更加符合规范。