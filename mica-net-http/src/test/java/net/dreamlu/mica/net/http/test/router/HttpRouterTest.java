package net.dreamlu.mica.net.http.test.router;

import net.dreamlu.mica.net.http.common.*;
import net.dreamlu.mica.net.http.common.router.HttpRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HttpRouter 单元测试
 */
class HttpRouterTest {

	private HttpRouter router;

	@BeforeEach
	void setUp() {
		router = new HttpRouter();
	}

	private HttpRequest createRequest(String path, Method method) {
		HttpRequest request = new HttpRequest();
		RequestLine requestLine = new RequestLine();
		requestLine.path = path;
		requestLine.method = method;
		requestLine.protocol = "HTTP";
		requestLine.version = "1.1";
		request.setRequestLine(requestLine);
		return request;
	}

	private HttpRequest createGetRequest(String path) {
		return createRequest(path, Method.GET);
	}

	// ==================== 基础路由测试 ====================

	@Test
	void testExactMatch() throws Exception {
		router.get("/hello", request -> ok("hello"));

		HttpResponse resp = router.handler(createGetRequest("/hello"));
		assertEquals(HttpResponseStatus.C200, resp.getStatus());
		assertEquals("hello", new String(resp.getBody()));
	}

	@Test
	void testRootPath() throws Exception {
		router.get("/", request -> ok("root"));

		HttpResponse resp = router.handler(createGetRequest("/"));
		assertEquals(HttpResponseStatus.C200, resp.getStatus());
	}

	@Test
	void testNotFound() throws Exception {
		router.get("/exists", request -> ok("exists"));

		HttpResponse resp = router.handler(createGetRequest("/not-exists"));
		assertEquals(HttpResponseStatus.C404, resp.getStatus());
	}

	@Test
	void testCustomNotFound() throws Exception {
		router.notFound(request -> {
			HttpResponse resp = new HttpResponse(request);
			resp.setStatus(HttpResponseStatus.C404);
			resp.setBody("custom 404".getBytes());
			return resp;
		});

		HttpResponse resp = router.handler(createGetRequest("/missing"));
		assertEquals(HttpResponseStatus.C404, resp.getStatus());
		assertEquals("custom 404", new String(resp.getBody()));
	}

	// ==================== HTTP Method 测试 ====================

	@Test
	void testHttpMethods() throws Exception {
		router.get("/resource", request -> ok("GET"));
		router.post("/resource", request -> ok("POST"));
		router.put("/resource", request -> ok("PUT"));
		router.delete("/resource", request -> ok("DELETE"));
		router.patch("/resource", request -> ok("PATCH"));
		router.head("/resource", request -> ok("HEAD"));
		router.options("/resource", request -> ok("OPTIONS"));

		assertEquals("GET", new String(router.handler(createRequest("/resource", Method.GET)).getBody()));
		assertEquals("POST", new String(router.handler(createRequest("/resource", Method.POST)).getBody()));
		assertEquals("PUT", new String(router.handler(createRequest("/resource", Method.PUT)).getBody()));
		assertEquals("DELETE", new String(router.handler(createRequest("/resource", Method.DELETE)).getBody()));
		assertEquals("PATCH", new String(router.handler(createRequest("/resource", Method.PATCH)).getBody()));
		assertEquals("HEAD", new String(router.handler(createRequest("/resource", Method.HEAD)).getBody()));
		assertEquals("OPTIONS", new String(router.handler(createRequest("/resource", Method.OPTIONS)).getBody()));
	}

	@Test
	void testAllMethodHandler() throws Exception {
		router.route("/any", request -> ok("any method"));

		for (Method method : Method.values()) {
			HttpResponse resp = router.handler(createRequest("/any", method));
			assertEquals(HttpResponseStatus.C200, resp.getStatus(), "Should match for method: " + method);
			assertEquals("any method", new String(resp.getBody()));
		}
	}

	@Test
	void testMethodFallback() throws Exception {
		router.get("/resource", request -> ok("GET"));
		router.route("/resource", request -> ok("ALL"));

		// GET 有专属 handler
		assertEquals("GET", new String(router.handler(createRequest("/resource", Method.GET)).getBody()));
		// DELETE 没有专属 handler，回退到 allMethodHandler
		assertEquals("ALL", new String(router.handler(createRequest("/resource", Method.DELETE)).getBody()));
	}

	// ==================== 路径参数测试 ====================

	@Test
	void testPathParam() throws Exception {
		router.get("/user/{id}", request -> ok("user:" + request.getPathParam("id")));

		HttpResponse resp = router.handler(createGetRequest("/user/123"));
		assertEquals("user:123", new String(resp.getBody()));
	}

	@Test
	void testMultiplePathParams() throws Exception {
		router.get("/user/{userId}/post/{postId}", request -> {
			return ok(request.getPathParam("userId") + ":" + request.getPathParam("postId"));
		});

		HttpResponse resp = router.handler(createGetRequest("/user/10/post/20"));
		assertEquals("10:20", new String(resp.getBody()));
	}

	@Test
	void testPathParamNotFound() throws Exception {
		router.get("/user/{id}", request -> ok("user:" + request.getPathParam("id")));

		// 路径不匹配（缺少参数段）
		HttpResponse resp = router.handler(createGetRequest("/user/"));
		assertEquals(HttpResponseStatus.C404, resp.getStatus());
	}

	// ==================== 通配符测试 ====================

	@Test
	void testWildcard() throws Exception {
		router.get("/static/**", request -> ok("static:" + request.getRequestLine().getPath()));

		HttpResponse resp = router.handler(createGetRequest("/static/js/app.js"));
		assertEquals(HttpResponseStatus.C200, resp.getStatus());
		assertEquals("static:/static/js/app.js", new String(resp.getBody()));
	}

	@Test
	void testWildcardAtRoot() throws Exception {
		router.get("/**", request -> ok("catch all"));

		HttpResponse resp = router.handler(createGetRequest("/any/path/here"));
		assertEquals(HttpResponseStatus.C200, resp.getStatus());
		assertEquals("catch all", new String(resp.getBody()));
	}

	// ==================== 过滤器测试 ====================

		@Test
	void testGlobalFilter() throws Exception {
		router.filter((request, chain) -> {
			HttpResponse resp = chain.doFilter(request);
			// 过滤器可以修改响应
			return resp;
		});
		router.get("/test", request -> ok("test"));

		HttpResponse resp = router.handler(createGetRequest("/test"));
		assertEquals(HttpResponseStatus.C200, resp.getStatus());
		assertEquals("test", new String(resp.getBody()));
	}

	@Test
	void testPathPatternFilter() throws Exception {
		router.filter("/api/**", (request, chain) -> {
			HttpResponse resp = new HttpResponse(request);
			resp.setStatus(HttpResponseStatus.C401);
			resp.setBody("unauthorized".getBytes());
			return resp;
		});
		router.get("/api/user", request -> ok("user"));

		// /api/** 匹配 /api/user
		HttpResponse resp = router.handler(createGetRequest("/api/user"));
		assertEquals(HttpResponseStatus.C401, resp.getStatus());
	}

	@Test
	void testPathPatternFilterNotMatch() throws Exception {
		router.filter("/api/**", (request, chain) -> {
			HttpResponse resp = new HttpResponse(request);
			resp.setStatus(HttpResponseStatus.C401);
			resp.setBody("unauthorized".getBytes());
			return resp;
		});
		router.get("/public/user", request -> ok("user"));

		// /public/user 不匹配 /api/**
		HttpResponse resp = router.handler(createGetRequest("/public/user"));
		assertEquals(HttpResponseStatus.C200, resp.getStatus());
	}

	// ==================== 异常处理测试 ====================

	@Test
	void testErrorHandler() throws Exception {
		router.error((request, error) -> {
			HttpResponse resp = new HttpResponse(request);
			resp.setStatus(HttpResponseStatus.C500);
			resp.setBody(("error:" + error.getMessage()).getBytes());
			return resp;
		});
		router.get("/error", request -> {
			throw new RuntimeException("test error");
		});

		HttpResponse resp = router.handler(createGetRequest("/error"));
		assertEquals(HttpResponseStatus.C500, resp.getStatus());
		assertTrue(new String(resp.getBody()).contains("test error"));
	}

	// ==================== 深度保护测试 ====================

	@Test
	void testMaxRouteDepth() {
		// 构建超过 32 层深度的路径
		StringBuilder deepPath = new StringBuilder("/");
		for (int i = 0; i < 40; i++) {
			deepPath.append("a").append("/");
		}
		router.get(deepPath.toString(), request -> ok("deep"));

		assertThrows(IllegalStateException.class, () -> {
			router.handler(createGetRequest(deepPath.toString()));
		});
	}

	// ==================== 辅助方法 ====================

	private HttpResponse ok(String body) {
		HttpResponse resp = new HttpResponse();
		resp.setStatus(HttpResponseStatus.C200);
		resp.setBody(body.getBytes());
		return resp;
	}

}
