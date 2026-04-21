package net.dreamlu.mica.net.http.common.router;

import net.dreamlu.mica.net.http.common.*;
import net.dreamlu.mica.net.http.common.handler.HttpRequestHandler;
import net.dreamlu.mica.net.utils.hutool.StrUtil;

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
	private static final int MAX_ROUTE_DEPTH = 32;

	private final TrieNode root = new TrieNode();
	private final List<FilterMapping> filters = new ArrayList<>();
	private RouteHandler notFoundHandler;
	private ErrorHandler errorHandler;

	public HttpRouter get(String path, RouteHandler handler) {
		return route(Method.GET, path, handler);
	}

	public HttpRouter post(String path, RouteHandler handler) {
		return route(Method.POST, path, handler);
	}

	public HttpRouter put(String path, RouteHandler handler) {
		return route(Method.PUT, path, handler);
	}

	public HttpRouter delete(String path, RouteHandler handler) {
		return route(Method.DELETE, path, handler);
	}

	public HttpRouter patch(String path, RouteHandler handler) {
		return route(Method.PATCH, path, handler);
	}

	public HttpRouter head(String path, RouteHandler handler) {
		return route(Method.HEAD, path, handler);
	}

	public HttpRouter options(String path, RouteHandler handler) {
		return route(Method.OPTIONS, path, handler);
	}

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
				if (node.wildcardChild == null) {
					node.wildcardChild = new TrieNode();
				}
				node = node.wildcardChild;
			} else if (PathUtils.isParam(segment)) {
				String paramName = PathUtils.extractParamName(segment);
				TrieNode paramNode = node.paramChildren.get(paramName);
				if (paramNode == null) {
					paramNode = new TrieNode();
					node.paramChildren.put(paramName, paramNode);
				}
				node = paramNode;
			} else {
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

	public HttpRouter route(String path, RouteHandler handler) {
		return route(null, path, handler);
	}

	public HttpRouter filter(String pathPattern, HttpFilter filter) {
		if (StrUtil.isBlank(pathPattern)) {
			throw new IllegalArgumentException("pathPattern 不能为空");
		}
		if (!pathPattern.startsWith("/")) {
			pathPattern = "/" + pathPattern;
		}
		filters.add(new FilterMapping(pathPattern, filter));
		return this;
	}

	public HttpRouter filter(HttpFilter filter) {
		return filter("/**", filter);
	}

	public HttpRouter notFound(RouteHandler handler) {
		this.notFoundHandler = handler;
		return this;
	}

	public HttpRouter error(ErrorHandler handler) {
		this.errorHandler = handler;
		return this;
	}

	@Override
	public HttpResponse handler(HttpRequest request) throws Exception {
		RequestLine requestLine = request.getRequestLine();
		String path = requestLine.getPath();
		Method method = requestLine.getMethod();

		Map<String, String> params = new HashMap<>();
		TrieNode matchNode = matchNode(root, PathUtils.splitPath(path), 0, params);

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

		for (Map.Entry<String, String> entry : params.entrySet()) {
			request.setAttribute(entry.getKey(), entry.getValue());
		}

		List<HttpFilter> matchedFilters = new ArrayList<>();
		for (FilterMapping fm : filters) {
			if (PathUtils.matchPattern(fm.pathPattern, path)) {
				matchedFilters.add(fm.filter);
			}
		}

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

	/**
	 * 前缀树递归匹配，深度超过 MAX_ROUTE_DEPTH 时抛异常防止 StackOverflow
	 */
	private TrieNode matchNode(TrieNode node, String[] segments, int index, Map<String, String> params) {
		if (index > MAX_ROUTE_DEPTH) {
			throw new IllegalStateException("路由深度超出最大允许值：" + MAX_ROUTE_DEPTH);
		}
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

		// 优先级 2: 参数匹配
		for (Map.Entry<String, TrieNode> entry : node.paramChildren.entrySet()) {
			TrieNode paramNode = entry.getValue();
			params.put(entry.getKey(), segment);
			TrieNode result = matchNode(paramNode, segments, index + 1, params);
			if (result != null && result.hasHandler()) {
				return result;
			}
			params.remove(entry.getKey());
		}

		// 优先级 3: 通配符
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
}
