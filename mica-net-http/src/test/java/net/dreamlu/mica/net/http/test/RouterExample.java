package net.dreamlu.mica.net.http.test;

import net.dreamlu.mica.net.http.common.HttpRequest;
import net.dreamlu.mica.net.http.common.HttpResponse;
import net.dreamlu.mica.net.http.common.HttpResponseStatus;
import net.dreamlu.mica.net.http.common.router.HttpRouter;
import net.dreamlu.mica.net.http.server.HttpServerStarter;

import java.nio.charset.StandardCharsets;

/**
 * HttpRouter 使用示例
 */
public class RouterExample {

	public static void main(String[] args) throws Exception {
		HttpRouter router = buildRouter();
		HttpServerStarter starter = new HttpServerStarter(8080, router);
		starter.start();
		System.out.println("Server started on http://localhost:8080");
	}

	public static HttpRouter buildRouter() {
		HttpRouter router = new HttpRouter();

		// 1. 基础路由
		router.get("/", request -> {
			return ok(request, "Hello World");
		});

		// 2. 路径参数
		router.get("/user/{id}", request -> {
			String id = request.getPathParam("id");
			return ok(request, "User: " + id);
		});

		router.get("/user/{userId}/post/{postId}", request -> {
			String userId = request.getPathParam("userId");
			String postId = request.getPathParam("postId");
			return ok(request, "User: " + userId + ", Post: " + postId);
		});

		// 3. RESTful 风格
		router.get("/api/user", request -> ok(request, "List users"));
		router.post("/api/user", request -> ok(request, "Create user"));
		router.put("/api/user/{id}", request -> ok(request, "Update user: " + request.getPathParam("id")));
		router.delete("/api/user/{id}", request -> ok(request, "Delete user: " + request.getPathParam("id")));

		// 4. 匹配所有 Method
		router.route("/health", request -> ok(request, "OK"));

		// 5. 通配符（静态资源）
		router.get("/static/**", request -> {
			String path = request.getRequestLine().getPath();
			return ok(request, "Static resource: " + path);
		});

		// 6. 全局过滤器
		router.filter((request, chain) -> {
			long start = System.currentTimeMillis();
			HttpResponse response = chain.doFilter(request);
			long cost = System.currentTimeMillis() - start;
			System.out.println("[LogFilter] " + request.getRequestLine() + " - " + cost + "ms");
			return response;
		});

		// 7. 路径模式过滤器
		router.filter("/api/**", (request, chain) -> {
			String token = request.getHeader("Authorization");
			if (token == null) {
				HttpResponse resp = new HttpResponse(request);
				resp.setStatus(HttpResponseStatus.C401);
				resp.setBody("Unauthorized".getBytes(StandardCharsets.UTF_8));
				return resp;
			}
			return chain.doFilter(request);
		});

		router.filter("/api/admin/**", (request, chain) -> {
			String role = request.getHeader("X-Admin-Role");
			if (!"admin".equals(role)) {
				HttpResponse resp = new HttpResponse(request);
				resp.setStatus(HttpResponseStatus.C403);
				resp.setBody("Forbidden".getBytes(StandardCharsets.UTF_8));
				return resp;
			}
			return chain.doFilter(request);
		});

		// 8. 自定义 404
		router.notFound(request -> {
			HttpResponse resp = new HttpResponse(request);
			resp.setStatus(HttpResponseStatus.C404);
			resp.setBody("Not Found: ".getBytes(StandardCharsets.UTF_8));
			return resp;
		});

		// 9. 异常处理
		router.error((request, error) -> {
			request.setAttribute("javax.servlet.error.exception", error);
			HttpResponse resp = new HttpResponse(request);
			resp.setStatus(HttpResponseStatus.C500);
			resp.setBody(("Error: " + error.getMessage()).getBytes(StandardCharsets.UTF_8));
			return resp;
		});

		return router;
	}

	private static HttpResponse ok(HttpRequest request, String body) {
		HttpResponse resp = new HttpResponse(request);
		resp.setStatus(HttpResponseStatus.C200);
		resp.setBody(body.getBytes(StandardCharsets.UTF_8));
		return resp;
	}
}
