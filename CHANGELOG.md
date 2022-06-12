# 变更记录

## 发行版本
### v0.0.1 - 2022-06-12
- 基于 t-io 3.8.1.v20220401-RELEASE 修改。 
- 使用 java8 作为最低编译版本。 
- 去除了一些使用不到的模块和代码。 
- 添加 mica 中的 HexUtils、DigestUtils、ExceptionUtils。 
- 切换到 cache2k、不依赖 fastjson。 
- 代码优化和规范，减少内存占用和提高性能。
- 解码失败次数可配置化，方便大包解析。