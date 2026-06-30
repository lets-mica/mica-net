# mica-net 使用文档（Useage）

本目录汇集 mica-net 各模块的"上手即用"文档，与代码示例保持一致。

| 模块 | 文档 | 说明 |
| ---- | ---- | ---- |
| mica-net-core | [TCP 使用](tcp.md) | 基于 `AsynchronousSocketChannel` 的 TCP 通信，含服务端/客户端、心跳、SSL、PROXY Protocol、集群 |
| mica-net-core | [UDP 使用](udp.md) | 基于 `DatagramChannel` 的 NIO UDP，含 `UdpChannel/UdpHandler` 三段式协议 |
| mica-net-http | [HTTP 使用](http.md) | `HttpServerStarter` + `HttpRouter` + SSE，覆盖路由/过滤器/异常/JSON |
| mica-net-http | [WebSocket 使用](websocket.md) | `WsServerStarter` + `IWsMsgHandler`，支持文本/二进制/握手扩展/主动推送 |

## 阅读建议

- 新接入 mica-net：先阅读 TCP 文档，理解 `TcpHandler`、三段式编解码、连接上下文；
- HTTP 服务（REST、SSE、流式响应）请直接看 [HTTP 使用](http.md)；
- 实时双向通信请看 [WebSocket 使用](websocket.md)；
- IoT、实时上报、低开销短报文请看 [UDP 使用](udp.md)；
- MCP / 集群 / 高级特性请参考对应源码模块。

## 反馈

文档勘误或补充欢迎提交 PR 或 issue。
