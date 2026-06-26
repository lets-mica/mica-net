# HTTP 与 WebSocket 统一 Starter 改造计划

> **状态**：待实施（下一版本）
> **范围**：`mica-net-http` 模块
> **目标**：让 HTTP 和 WebSocket 共用同一 Starter、同一端口、同一路由表，并支持在 `HttpRouter` 上直接注册 WS 端点

---

## 背景

当前 `mica-net-http` 模块内并存两套独立的启动入口和路由体系：

- **HTTP 入口**：[HttpServerStarter](file:///e:/codes/micax/mica-net/mica-net-http/src/main/java/net/dreamlu/mica/net/http/server/HttpServerStarter.java) + [HttpTioServerHandler](file:///e:/codes/micax/mica-net/mica-net-http/src/main/java/net/dreamlu/mica/net/http/server/HttpTioServerHandler.java) + [HttpTioServerListener](file:///e:/codes/micax/mica-net/mica-net-http/src/main/java/net/dreamlu/mica/net/http/server/HttpTioServerListener.java)
- **WebSocket 入口**：[WsServerStarter](file:///e:/codes/micax/mica-net/mica-net-http/src/main/java/net/dreamlu/mica/net/websocket/server/WsServerStarter.java) + [WsTioServerHandler](file:///e:/codes/micax/mica-net/mica-net-http/src/main/java/net/dreamlu/mica/net/websocket/server/WsTioServerHandler.java) + [WsTioServerListener](file:///e:/codes/micax/mica-net/mica-net-http/src/main/java/net/dreamlu/mica/net/websocket/server/WsTioServerListener.java)
- **HTTP 路由**：[HttpRouter](file:///e:/codes/micax/mica-net/mica-net-http/src/main/java/net/dreamlu/mica/net/http/common/router/HttpRouter.java)（仅 `HttpRequestHandler`）

业务方若要同时暴露 HTTP API 和 WebSocket 端点，要么绑定两个端口，要么自行在 HTTP handler 中手写 Upgrade 判定，体感割裂。

## 核心判断

**两件事都好做，且改造成本可控：**

1. **统一 Starter 可行**：`WsTioServerHandler.decode` 内部已经走了一次完整 HTTP 解码（[WsTioServerHandler.java#L264-L266](file:///e:/codes/micax/mica-net/mica-net-http/src/main/java/net/dreamlu/mica/net/websocket/server/WsTioServerHandler.java#L264-L266)），"是否升级"完全收敛在 `updateWebSocketProtocol`（[WsTioServerHandler.java#L454-L488](file:///e:/codes/micax/mica-net/mica-net-http/src/main/java/net/dreamlu/mica/net/websocket/server/WsTioServerHandler.java#L454-L488)）。在此基础上叠加一层 `HttpRequestHandler` 委托，即可让同一个端口同时跑 HTTP 和 WS。
2. **HttpRouter 支持 WS 端点可行**：Trie 节点结构（`children` / `paramChildren` / `wildcardChild`）与 `RouteHandler` 是松耦合的，再加一个 `wsRoutes` 映射即可。匹配到 WS 端点时通过特殊标记让上层 handler 接管握手。

---

## 目标架构

```
HttpWsServerStarter (新增)
└── WsTioServerHandler (复用 + 扩展)
    ├── decode 阶段
    │   ├── HTTP 请求 (无 Upgrade 头)   → 当作普通 HTTP 走 router
    │   ├── HTTP + Upgrade 头          → WS 握手，匹配 WsRoute
    │   └── 已握手的 WS 帧             → 直接走已注册 WsHandler
    ├── handler 阶段
    │   ├── HttpRequest → httpRequestHandler.handler(request)
    │   └── WsRequest   → wsHandler.onText / onBytes / onClose
    └── encode 阶段
        ├── HttpResponse → HttpResponseEncoder.encode
        └── WsResponse   → WsServerEncoder.encode / 握手包走 HttpResponseEncoder
```

---

## 改造步骤

### 阶段 1：扩展 `HttpRouter` 支持 WS 端点

- [ ] 新增 `WsHandler` 接口（替代或包装 `IWsMsgHandler` 的子集），与 `RouteHandler` 平级：
  ```java
  public interface WsHandler {
      void onOpen(WsSession session, HttpRequest request) throws Exception;
      void onText(WsSession session, String text) throws Exception;
      void onBytes(WsSession session, byte[] bytes) throws Exception;
      void onClose(WsSession session) throws Exception;
  }
  ```
- [ ] 在 [HttpRouter.java](file:///e:/codes/micax/mica-net/mica-net-http/src/main/java/net/dreamlu/mica/net/http/common/router/HttpRouter.java) 中扩展 `TrieNode`，新增 `wsHandlers` 字段（与现有 `handlers` 互斥）
- [ ] 新增 `HttpRouter.ws(String path, WsHandler handler)` 注册方法
- [ ] 路径匹配到 WS 端点时返回特殊标记（如 `null` + 标记位，或抛 `WsRouteMatchException`），由上层 handler 接手
- [ ] 路径参数在 `router.handler` 阶段已经写入 `request.setAttribute(...)`，握手成功后保存到 `WsSessionContext`，`WsHandler` 回调里可直接 `session.getPathParam("id")`
- [ ] 启动期检查 `get("/ws")` 与 `ws("/ws")` 的冲突，WS 注册优先
- [ ] 编写单元测试覆盖路径匹配、参数解析、冲突检测

### 阶段 2：新增统一 `HttpWsServerStarter`

- [ ] 新增 [HttpWsServerStarter.java](file:///e:/codes/micax/mica-net/mica-net-http/src/main/java/net/dreamlu/mica/net/http/server/HttpWsServerStarter.java)
  - 复用 `WsTioServerStarter` 的配置路径
  - 默认配置以 WS 为基准：长连接、`setHeartbeatTimeout(0)`、`SnowflakeTioUuid`、readBuffer 取两者较大值
  - HTTP 短连接关闭仍由 listener 处理
- [ ] 扩展 [WsTioServerHandler.java](file:///e:/codes/micax/mica-net/mica-net-http/src/main/java/net/dreamlu/mica/net/websocket/server/WsTioServerHandler.java)：
  - `decode` 阶段在 `updateWebSocketProtocol` 之前判定是否为纯 HTTP 请求
  - `handler` 阶段根据 `Packet` 类型分发：`HttpRequest` → `httpRequestHandler.handler()`，`WsRequest` → `wsHandler.onText/onBytes/onClose`
  - `encode` 阶段新增 `instanceof HttpResponse` 分支走 `HttpResponseEncoder.encode`
- [ ] 合并 [HttpTioServerListener.onAfterSent](file:///e:/codes/micax/mica-net/mica-net-http/src/main/java/net/dreamlu/mica/net/http/server/HttpTioServerListener.java) 的短连接关闭逻辑到 `WsTioServerListener`，对 `HttpResponse` 走短连接、对 `WsResponse` 不动

### 阶段 3：保持向后兼容

- [ ] 保留 `HttpServerStarter` 与 `WsServerStarter` 现有 API 不变
- [ ] `IWsMsgHandler` 仍然可用，通过 `WsHandlerAdapter`（桥接）注册到 `router.ws(...)`
- [ ] 现有 `HttpRouter.get/post/...` API 不动
- [ ] `module-info.java` 新增 `HttpWsServerStarter` 所在包的 `exports`

### 阶段 4：测试与文档

- [ ] 端到端测试：同一端口同时承载 HTTP API 与 WS 端点
- [ ] WS 握手失败的降级行为（缺失 `Sec_WebSocket_Key` 但带 `Upgrade: websocket` → 返回 HTTP 400）
- [ ] 路径参数透传到 `WsHandler` 的正确性
- [ ] `Sec-WebSocket-Protocol` 子协议协商链路（`IWsSubProtocolsMsgHandler.encodeSubProtocol`）保留
- [ ] 更新 [RouterExample.java](file:///e:/codes/micax/mica-net/mica-net-http/src/test/java/net/dreamlu/mica/net/http/test/RouterExample.java) 增加 WS 端点注册示例
- [ ] 更新 [McpTest.java](file:///e:/codes/micax/mica-net/mica-net-http/src/test/java/net/dreamlu/mica/net/http/test/McpTest.java) 视情况补充用例

---

## 复杂度评估

| 项目 | 评估 |
|------|------|
| 工作量 | 中等。`HttpWsServerStarter` ~100 行；`WsTioServerHandler` 扩展 HTTP 分发 ~150 行；`HttpRouter` 增加 wsRoutes 段 ~50 行；`WsHandler` 抽象 + adapter ~80 行 |
| 性能 | 零损耗。HTTP 路径少走一次"先升级失败再回退"的逻辑，理论上比同时启动两个端口更省一次 socket |
| API 一致性 | 显著提升——一个 starter、一个 router、一次 `start()`，对齐 Spring/Vert.x 习惯 |
| 向后兼容 | 高。现有 starter 不删，新 starter 走新接口 |
| 配置决策 | 唯一需要确认默认的 `shortConnection` / `heartbeatTimeout` / `readBufferSize` 取哪一套（建议以 WS 为主，HTTP 短连接交给 listener） |

---

## 风险与边界情况

1. **`Sec_WebSocket_Key` 缺失但带了 `Upgrade: websocket`**：当前 `WsTioServerHandler` 直接抛 `TioDecodeException`，建议放宽为"当 HTTP 处理并返回 400"。
2. **路径同时存在 `get("/ws")` 与 `ws("/ws")`**：应在启动期检查冲突，WS 注册优先并给出明确错误信息。
3. **`Sec-WebSocket-Protocol` 子协议协商**：走 `IWsSubProtocolsMsgHandler.encodeSubProtocol`，统一 starter 时需保留此链路。
4. **WS 路径的 404**：应在握手前返回 HTTP 404 或 426 Upgrade Required，不要进入 WS 上下文。
5. **`HttpFilter` 对 WS 握手前的复用**：多数 filter 关心的鉴权/CORS 在握手前完成是合理的。握手后的消息流是否要可过滤，建议留到下一迭代单独评估。

---

## 落地顺序建议

1. **先做 `WsHandler` 抽象 + `HttpRouter.ws(...)`**，纯 HTTP 端走原路径不动。立即为"两个独立端口"场景带来一致的写法，单测最容易。
2. **再做 `HttpWsServerStarter`**，复用 `WsTioServerHandler` 的 decode + 扩展 handler 分发。
3. **最后考虑 `HttpFilter` 对 WS 握手前的复用**，作为可选增强。

---

## 后续若要重启该计划

1. 确认默认配置策略（短连接 / 心跳 / readBuffer）
2. 敲定 `WsHandler` 与 `IWsMsgHandler` 的关系（保留 / 包装 / 替代）
3. 评估 `WsSession` 抽象的边界（是否暴露 `ChannelContext`）
4. 决定 `HttpFilter` 是否纳入本次改造
