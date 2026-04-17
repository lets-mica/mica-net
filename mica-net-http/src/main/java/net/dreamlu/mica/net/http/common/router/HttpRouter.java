package net.dreamlu.mica.net.http.common.router;

import net.dreamlu.mica.net.http.common.HttpRequest;
import net.dreamlu.mica.net.http.common.HttpResponse;
import net.dreamlu.mica.net.http.common.HttpResponseStatus;
import net.dreamlu.mica.net.http.common.Method;
import net.dreamlu.mica.net.http.common.handler.HttpRequestHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP 路由器，基于前缀树实现高性能路由匹配
 *
 * @author L.cm
 */
public class HttpRouter implements HttpRequestHandler {
	private final TrieNode root = new TrieNode();
	private final List<FilterMapping> filters = new ArrayList<>();
	private RouteHandler notFoundHandler;
	private ErrorHandler errorHandler;

	/**
	 * 添加 GET 路由
	 *
	 * @param path   路径
	 * @param handler 处理器
	 * @return this
	 */
	public HttpRouter get(String path, RouteHandler handler) {
		return route(Method.GET, path, handler);
	}

	/**
	 * 添加 POST 路由
	 *
	 * @param path   路径
	 * @param handler 处理器
	 * @return this
	 */
	public HttpRouter post(String path, RouteHandler handler) {
		return route(Method.POST, path, handler);
	}

	/**
	 * 添加 PUT 路由
	 *
	 * @param path   路径
	 * @param handler 处理器
	 * @return this
	 */
	public HttpRouter put(String path, RouteHandler handler) {
		return route(Method.PUT, path, handler);
	}

	/**
	 * 添加 DELETE 路由
	 *
	 * @param path   路径
	 * @param handler 处理器
	 * @return this
	 */
	public HttpRouter delete(String path, RouteHandler handler) {
		return route(Method.DELETE, path, handler);
	}

	/**
	 * 添加 PATCH 路由
	 *
	 * @param path   路径
	 * @param handler 处理器
	 * @return this
	 */
	public HttpRouter patch(String path, RouteHandler handler) {
		return route(Method.PATCH, path, handler);
	}

	/**
	 * 添加 HEAD 路由
	 *
	 * @param path   路径
	 * @param handler 处理器
	 * @return this
	 */
	public HttpRouter head(String path, RouteHandler handler) {
		return route(Method.HEAD, path, handler);
	}

	/**
	 * 添加 OPTIONS 路由
	 *
	 * @param path   路径
	 * @param handler 处理器
	 * @return this
	 */
	public HttpRouter options(String path, RouteHandler handler) {
		return route(Method.OPTIONS, path, handler);
	}

	/**
	 * 添加指定 Method 的路由
	 *
	 * @param method  HTTP Method
	 * @param path    路径
	 * @param handler 处理器
	 * @return this
	 */
	public HttpRouter route(Method method, String path, RouteHandler handler) {
		if (path == null || path.isEmpty()) {
			throw new IllegalArgumentException("path cannot be null or empty");
		}
		if (!path.startsWith("/")) {
			path = "/" + path;
		}

		String[] segments = PathUtils.splitPath(path);
		TrieNode node = root;
		for (String segment : segments) {
			if (PathUtils.isWildcard(segment)) {
				// 通配符
				if (node.wildcardChild == null) {
					node.wildcardChild = new TrieNode();
				}
				node = node.wildcardChild;
			} else if (PathUtils.isParam(segment)) {
				// 路径参数
				String paramName = PathUtils.extractParamName(segment);
				TrieNode paramNode = node.paramChildren.get(paramName);
				if (paramNode == null) {
					paramNode = new TrieNode();
					node.paramChildren.put(paramName, paramNode);
				}
				node = paramNode;
			} else {
				// 精确匹配
				TrieNode child = node.children.get(segment);
				if (child == null) {
					child = new TrieNode();
					node.children.put(segment, child);
				}
				node = child;
			}
		}
		node.addHandler(method, handler);
		return this;
	}

	/**
	 * 添加匹配所有 Method 的路由
	 *
	 * @param path   路径
	 * @param handler 处理器
	 * @return this
	 */
	public HttpRouter route(String path, RouteHandler handler) {
		return route(null, path, handler);
	}

	/**
	 * 添加过滤器，按路径模式匹配
	 *
	 * @param pathPattern 路径模式，如 "/api/**"、"/user/list"
	 * @param filter      过滤器
	 * @return this
	 */
	public HttpRouter filter(String pathPattern, HttpFilter filter) {
		if (pathPattern == null || pathPattern.isEmpty()) {
			throw new IllegalArgumentException("pathPattern cannot be null or empty");
		}
		if (!pathPattern.startsWith("/")) {
			pathPattern = "/" + pathPattern;
		}
		filters.add(new FilterMapping(pathPattern, filter));
		return this;
	}

	/**
	 * 添加全局过滤器，匹配所有路径
	 *
	 * @param filter 过滤器
	 * @return this
	 */
	public HttpRouter filter(HttpFilter filter) {
		return filter("/**", filter);
	}

	/**
	 * 设置 404 处理器
	 *
	 * @param handler 处理器
	 * @return this
	 */
	public HttpRouter notFound(RouteHandler handler) {
		this.notFoundHandler = handler;
		return this;
	}

	/**
	 * 设置异常处理器
	 *
	 * @param handler 处理器
	 * @return this
	 */
	public HttpRouter error(ErrorHandler handler) {
		this.errorHandler = handler;
		return this;
	}

	@Override
	public HttpResponse handler(HttpRequest request) throws Exception {
		String path = request.getRequestLine().getPath();
		Method method = request.getRequestLine().getMethod();

		// 1. 前缀树匹配
		Map<String, String> params = new HashMap<>();
		TrieNode matchNode = matchNode(root, PathUtils.splitPath(path), 0, params);

		// 2. 匹配失败 → 404
		if (matchNode == null || !matchNode.hasHandler()) {
			if (notFoundHandler != null) {
				return notFoundHandler.handle(request);
			}
			HttpResponse resp = new HttpResponse(request);
			resp.setStatus(HttpResponseStatus.C404);
			return resp;
		}

		RouteHandler handler = matchNode.getHandler(method);
		if (handler == null) {
			// Method 不匹配，尝试 allMethodHandler
			handler = matchNode.allMethodHandler;
		}
		if (handler == null) {
			if (notFoundHandler != null) {
				return notFoundHandler.handle(request);
			}
			HttpResponse resp = new HttpResponse(request);
			resp.setStatus(HttpResponseStatus.C404);
			return resp;
		}

		// 3. 写入路径参数到 request
		for (Map.Entry<String, String> entry : params.entrySet()) {
			request.setAttribute(entry.getKey(), entry.getValue());
		}

		// 4. 收集匹配的过滤器
		List<HttpFilter> matchedFilters = new ArrayList<>();
		for (FilterMapping fm : filters) {
			if (PathUtils.matchPattern(fm.pathPattern, path)) {
				matchedFilters.add(fm.filter);
			}
		}

		// 5. 构建过滤器链并执行
		HttpFilterChain chain = new HttpFilterChain(matchedFilters, handler);
		try {
			return chain.doFilter(request);
		} catch (Exception e) {
			if (errorHandler != null) {
				return errorHandler.handle(request, e);
			}
			throw e;
		}
	}

	private TrieNode matchNode(TrieNode node, String[] segments, int index, Map<String, String> params) {
		if (index == segments.length) {
			return node;
		}

		String segment = segments[index];

		// 优先级 1: 精确匹配
		TrieNode child = node.children.get(segment);
		if (child != null) {
			TrieNode result = matchNode(child, segments, index + 1, params);
			if (result != null && result.hasHandler()) {
				return result;
			}
		}

		// 优先级 2: 参数匹配（遍历所有参数子节点）
		for (Map.Entry<String, TrieNode> entry : node.paramChildren.entrySet()) {
			TrieNode paramNode = entry.getValue();
			params.put(entry.getKey(), segment);
			TrieNode result = matchNode(paramNode, segments, index + 1, params);
			if (result != null && result.hasHandler()) {
				return result;
			}
			params.remove(entry.getKey()); // 回溯
		}

		// 优先级 3: 通配符匹配
		if (node.wildcardChild != null) {
			return node.wildcardChild;
		}

		return null;
	}

	private static class FilterMapping {
		final String pathPattern;
		final HttpFilter filter;

		FilterMapping(String pathPattern, HttpFilter filter) {
			this.pathPattern = pathPattern;
			this.filter = filter;
		}
	}

	/**
	 * 路由信息，用于打印
	 */
	public static class RouteInfo {
		public final String methods;
		public final String path;

		public RouteInfo(String methods, String path) {
			this.methods = methods;
			this.path = path;
		}

		@Override
		public String toString() {
			return methods + "  " + path;
		}
	}

}
