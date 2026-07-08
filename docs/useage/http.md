# mica-net HTTP 使用文档

> mica-net-http 基于 mica-net-core + 内置 HTTP 编解码，提供 `HttpServerStarter`、`HttpRouter`、`HttpStream`（SSE）等能力，开箱即用。

## 1. 引入依赖

```xml
<dependency>
    <groupId>net.dreamlu</groupId>
    <artifactId>mica-net-http</artifactId>
    <version>${mica-net.version}</version>
</dependency>
```

## 2. 核心概念

| 名称 | 作用 |
| ---- | ---- |
| `HttpServerStarter` | 一键启动 HTTP 服务，封装了 `TioServer` + HTTP 编解码 |
| `HttpRequestHandler` | **函数式接口**，业务只需实现 `handler(HttpRequest)` 返回 `HttpResponse` |
| `HttpRequest` | 请求：包含 `requestLine`、`headers`、`cookies`、`body`、`pathParam` 等 |
| `HttpResponse` | 响应：链式设 header、cookie、status、body |
| `HttpRouter` | 轻量级路由（基于 Trie），支持路径参数 + 过滤器 + 全局异常处理 |
| `HttpStream` | 服务端推送：`startSse` 实现 SSE、chunked 等 |
| `HttpConfig` | 配置：端口、缓存大小、最大请求头、SSL、PROXY protocol 等 |

## 3. Hello World

实现一个最简单的 handler：

```java
import net.dreamlu.mica.net.http.common.*;
import net.dreamlu.mica.net.http.common.handler.HttpRequestHandler;
import net.dreamlu.mica.net.http.server.HttpServerStarter;

public class HelloServer {
    public static void main(String[] args) throws Exception {
        HttpRequestHandler handler = request -> {
            HttpResponse resp = new HttpResponse(request);
            resp.setStatus(HttpResponseStatus.C200);
            resp.setBody("Hello mica-net-http".getBytes());
            return resp;
        };
        HttpServerStarter starter = new HttpServerStarter(8080, handler);
        starter.start();

        System.out.println("http://localhost:8080");
    }
}
```

```bash
curl http://localhost:8080
# Hello mica-net-http
```

## 4. HttpRouter 用法

路由以 `HttpRouter` 形式注册，对小服务/内部接口已足够用：

```java
HttpRouter router = new HttpRouter();

// 基础路由 + 路径参数
router.get("/", (req) -> ok(req, "Hello World"));
router.get("/user/{id}", req -> ok(req, "User " + req.getPathParam("id")));

// RESTful
router.get("/api/user",    req -> ok(req, "list users"));
router.post("/api/user",   req -> ok(req, "create"));
router.put("/api/user/{id}", req -> ok(req, "update " + req.getPathParam("id")));
router.delete("/api/user/{id}", req -> ok(req, "delete " + req.getPathParam("id")));

// 任意 method
router.route("/health", req -> ok(req, "OK"));

// 通配符（静态资源代理）
router.get("/static/**", req -> ok(req, "Static: " + req.getRequestLine().getPath()));

new HttpServerStarter(8080, router).start();
```

```java
private static HttpResponse ok(HttpRequest request, String body) {
    HttpResponse resp = new HttpResponse(request);
    resp.setStatus(HttpResponseStatus.C200);
    resp.setBody(body.getBytes());
    return resp;
}
```

完整 200+ 行示例见 `mica-net-http/src/test/java/.../RouterExample.java`。

## 5. 过滤器 & 异常处理

```java
// 1. 全局过滤器（日志、鉴权链等）
router.filter((request, chain) -> {
    long t0 = System.currentTimeMillis();
    HttpResponse response = chain.doFilter(request);
    System.out.println("[log] " + request.getRequestLine() + " cost=" + (System.currentTimeMillis() - t0) + "ms");
    return response;
});

// 2. 路径模式过滤器：未带 Authorization 直接 401
router.filter("/api/**", (request, chain) -> {
    if (request.getHeader("Authorization") == null) {
        HttpResponse resp = new HttpResponse(request);
        resp.setStatus(HttpResponseStatus.C401);
        resp.setBody("Unauthorized".getBytes());
        return resp;
    }
    return chain.doFilter(request);
});

// 3. 自定义 404
router.notFound(request -> {
    HttpResponse resp = new HttpResponse(request);
    resp.setStatus(HttpResponseStatus.C404);
    return resp;
});

// 4. 全局异常处理
router.error((request, error) -> {
    HttpResponse resp = new HttpResponse(request);
    resp.setStatus(HttpResponseStatus.C500);
    resp.setBody(("Error: " + error.getMessage()).getBytes());
    return resp;
});
```

## 6. 常用 HTTP API

### 读请求

```java
// 路径 / query / body
RequestLine line = request.getRequestLine();
String path    = line.getPath();                  // /user/123
String query   = line.getQueryString();           // a=1&b=2
byte[] body    = request.getBody();
String bodyStr = request.getBodyString();

// Headers
String ua = request.getHeader("User-Agent");
String ip = request.getClientIp();

// Cookie（懒解析）
Map<String, Cookie> cookies = request.getCookieMap();

// 路由参数
String id = request.getPathParam("id");

// 转发（内部 path 跳转，不重新走 HTTP 层）
HttpResponse forward = request.forward("/internal/route");
```

### 写响应

```java
HttpResponse resp = new HttpResponse(request);
resp.setStatus(HttpResponseStatus.C200);
resp.setHeader(HeaderName.Content_Type, HeaderValue.from("application/json"));
resp.addHeader("X-Trace-Id", "abc123");
resp.addCookie(new Cookie("sid", "xyz"));
resp.setBody("{\"ok\":true}".getBytes());
return resp;
```

### 主动关闭

```java
request.close();                 // 服务端短连接场景
request.close("client abort");   // 带 remark
```

## 7. SSE / 流式响应

`HttpStream` 同时用于 SSE 与 chunked：

```java
@Override
public HttpResponse handler(HttpRequest request) throws Exception {
    HttpResponse response = new HttpResponse(request);
    response.addHeader(HeaderName.Access_Control_Allow_Origin, HeaderValue.from("*"));
    HttpStream stream = response.startSse(request);

    // 包实际写入管道后才推送，否则会被忽略
    response.setPacketListener((ctx, packet, ok) -> {
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                stream.send(i, "counter", "Count: " + i);
                ThreadUtils.sleep(1000);
            }
            stream.close();
        }).start();
    });
    return response;
}
```

完整示例见 `mica-net-http/src/test/java/.../SseExample.java`。

## 8. JSON 工具（mica-net-utils）

```java
import net.dreamlu.mica.net.utils.json.JsonUtil;

User u = JsonUtil.readValue(body, User.class);
String json = JsonUtil.toJson(u);
```

可选后端：Jackson2、Jackson3、Fastjson1、Fastjson2、Gson、Hutool-JSON、Snack3、Snack4。

## 9. SSL / PROXY Protocol

```java
HttpConfig cfg = new HttpConfig();
cfg.setSslConfig(SslConfig.forServer("/path/to/keystore.jks", "password"));
cfg.setProxyProtocolDecoder(true); // 启用 PROXY protocol v1/v2 解析
new HttpServerStarter(new Node(null, 443), cfg, handler).start();
```

| 字段 | 默认 | 含义 |
| ---- | ---- | ---- |
| `port` | - | 监听端口（与 `Node` 互斥，由构造方法二选一） |
| `maxHeaderLength` | `8 KB` | 单个 Header 上限 |
| `maxBodyLength` | - | 请求体上限 |
| `proxyProtocolDecoder` | `false` | 是否解析 PROXY protocol 头 |
| `sslConfig` | `null` | 开启 HTTPS |

## 10. 常见问题

- **短连接**：HTTP 默认按短连接处理（`setShortConnection(true)`），如需长连接自定义。
- **Body 解析**：框架会自动按 `Content-Type` 解析表单/JSON/Multipart；可在 `handler` 中直接取 `request.getBody()`。
- **跨域**：手动 `response.addHeader(HeaderName.Access_Control_Allow_Origin, HeaderValue.from("*"))` 即可，无需额外拦截器。

## 11. 完整 Demo

- `mica-net-http/src/test/java/.../HttpTest.java` —— 基础 handler
- `mica-net-http/src/test/java/.../RouterExample.java` —— 全功能路由
- `mica-net-http/src/test/java/.../SseExample.java` —— SSE 三种用法
- `mica-net-http/src/test/java/.../HttpBenchmark.java` —— 性能测试参考
