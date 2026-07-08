package net.dreamlu.mica.net.http.common.router;

import net.dreamlu.mica.net.http.common.*;
import net.dreamlu.mica.net.http.common.handler.HttpRequestHandler;
import net.dreamlu.mica.net.utils.hutool.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP 路由器，基于前缀树（Trie）实现高性能路由匹配。
 *
 * <p>支持以下特性：
 * <ul>
 *   <li>按 HTTP Method 注册路由：{@link #get(String, RouteHandler)}、{@link #post(String, RouteHandler)} 等便捷方法，
 *       或使用 {@link #route(Method, String, RouteHandler)} 统一注册</li>
 *   <li>路径段支持三种匹配模式，匹配优先级从高到低依次为：
 *     <ol>
 *       <li>精确匹配：普通字符串段，如 {@code /api/user}</li>
 *       <li>参数匹配：{@code {name}} 形式的占位符，匹配后可通过 {@link HttpRequest#getAttribute(String)} 获取</li>
 *       <li>通配符：{@code **}，匹配单层任意内容（注意：当前实现在路径段上匹配，行为等价于前缀或单层通配）</li>
 *     </ol>
 *   </li>
 *   <li>过滤器链：按注册顺序对匹配路径的请求执行 {@link HttpFilter}，支持 {@code /path/**}、{@code /path/*} 形式的 Ant 风格模式</li>
 *   <li>404 与全局异常处理：未匹配或抛出异常时分别回调 {@link #notFound(RouteHandler)} 与 {@link #error(ErrorHandler)}</li>
 * </ul>
 *
 * <p>典型用法：
 * <pre>{@code
 * HttpRouter router = new HttpRouter();
 * router.get("/api/users", req -> ...)
 *       .get("/api/users/{id}", req -> ...)
 *       .filter("/api/**", authFilter)
 *       .notFound(req -> new HttpResponse(req).setStatus(HttpResponseStatus.C404))
 *       .error((req, e) -> new HttpResponse(req).setStatus(HttpResponseStatus.C500));
 * }</pre>
 *
 * <p>本类非线程安全，路由注册与请求处理通常在不同阶段使用，请避免在请求处理过程中动态修改路由表。
 *
 * @author L.cm
 */
public class HttpRouter implements HttpRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(HttpRouter.class);
	/**
	 * 路由前缀树最大深度限制，防止恶意或异常的深层路径导致栈溢出（StackOverflowError）。
	 * 超出该深度将抛出 {@link IllegalStateException}。
	 */
	private static final int MAX_ROUTE_DEPTH = 32;

	/**
	 * 路由前缀树根节点，路径段从根节点开始逐级匹配。
	 */
	private final TrieNode root = new TrieNode();
	/**
	 * 全局过滤器列表，按注册顺序生效；执行时仅匹配路径模式命中的过滤器。
	 */
	private final List<FilterMapping> filters = new ArrayList<>();
	/**
	 * 404 兜底处理器；当无路由命中或 Method 不匹配且无 allMethodHandler 兜底时调用；为 null 时返回默认 404 响应。
	 */
	private RouteHandler notFoundHandler;
	/**
	 * 全局异常处理器；处理过程中抛出的异常会回调此处理器；为 null 时记录日志并返回 500 响应。
	 */
	private ErrorHandler errorHandler;

	/**
	 * 注册一个 HTTP GET 路由。
	 *
	 * @param path    请求路径，例如 {@code "/api/users"}；若不以 {@code /} 开头将自动补全
	 * @param handler 路由处理器
	 * @return 当前路由器实例，支持链式调用
	 */
	public HttpRouter get(String path, RouteHandler handler) {
		return route(Method.GET, path, handler);
	}

	/**
	 * 注册一个 HTTP POST 路由。
	 *
	 * @param path    请求路径，例如 {@code "/api/users"}
	 * @param handler 路由处理器
	 * @return 当前路由器实例，支持链式调用
	 */
	public HttpRouter post(String path, RouteHandler handler) {
		return route(Method.POST, path, handler);
	}

	/**
	 * 注册一个 HTTP PUT 路由。
	 *
	 * @param path    请求路径，例如 {@code "/api/users/{id}"}
	 * @param handler 路由处理器
	 * @return 当前路由器实例，支持链式调用
	 */
	public HttpRouter put(String path, RouteHandler handler) {
		return route(Method.PUT, path, handler);
	}

	/**
	 * 注册一个 HTTP QUERY 路由。
	 *
	 * @param path    请求路径，例如 {@code "/api/search"}
	 * @param handler 路由处理器
	 * @return 当前路由器实例，支持链式调用
	 */
	public HttpRouter query(String path, RouteHandler handler) {
		return route(Method.QUERY, path, handler);
	}

	/**
	 * 注册一个 HTTP DELETE 路由。
	 *
	 * @param path    请求路径，例如 {@code "/api/users/{id}"}
	 * @param handler 路由处理器
	 * @return 当前路由器实例，支持链式调用
	 */
	public HttpRouter delete(String path, RouteHandler handler) {
		return route(Method.DELETE, path, handler);
	}

	/**
	 * 注册一个 HTTP PATCH 路由。
	 *
	 * @param path    请求路径，例如 {@code "/api/users/{id}"}
	 * @param handler 路由处理器
	 * @return 当前路由器实例，支持链式调用
	 */
	public HttpRouter patch(String path, RouteHandler handler) {
		return route(Method.PATCH, path, handler);
	}

	/**
	 * 注册一个 HTTP HEAD 路由。
	 *
	 * @param path    请求路径，例如 {@code "/api/users"}
	 * @param handler 路由处理器
	 * @return 当前路由器实例，支持链式调用
	 */
	public HttpRouter head(String path, RouteHandler handler) {
		return route(Method.HEAD, path, handler);
	}

	/**
	 * 注册一个 HTTP OPTIONS 路由。
	 *
	 * @param path    请求路径，例如 {@code "/api/users"}
	 * @param handler 路由处理器
	 * @return 当前路由器实例，支持链式调用
	 */
	public HttpRouter options(String path, RouteHandler handler) {
		return route(Method.OPTIONS, path, handler);
	}

	/**
	 * 注册一个指定 HTTP Method 的路由。
	 *
	 * <p>路径会按 {@code /} 切分为段，并按段构建前缀树节点。
	 * 路径段支持以下形式：
	 * <ul>
	 *   <li>普通字符串：精确匹配</li>
	 *   <li>{@code {name}}：参数占位符，匹配单段任意内容，并在匹配时将实际值写入
	 *       {@link HttpRequest#setAttribute(String, Serializable)}，可通过 {@link HttpRequest#getAttribute(String)} 获取</li>
	 *   <li>{@code **}：通配符段</li>
	 * </ul>
	 *
	 * <p>同一 Method 重复注册同一路径将抛出 {@link IllegalArgumentException}。
	 *
	 * @param method  HTTP Method，为 {@code null} 时表示匹配所有 Method
	 * @param path    请求路径，不能为 null 或空字符串；若不以 {@code /} 开头将自动补全
	 * @param handler 路由处理器
	 * @return 当前路由器实例，支持链式调用
	 * @throws IllegalArgumentException 当 path 为 null 或空字符串时抛出
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

	/**
	 * 注册一个匹配所有 HTTP Method 的路由。等价于 {@code route(null, path, handler)}。
	 *
	 * @param path    请求路径
	 * @param handler 路由处理器
	 * @return 当前路由器实例，支持链式调用
	 * @see #route(Method, String, RouteHandler)
	 */
	public HttpRouter route(String path, RouteHandler handler) {
		return route(null, path, handler);
	}

	/**
	 * 注册一个路径模式过滤器。
	 *
	 * <p>路径模式支持以下形式（参见 {@link PathUtils#matchPattern(String, String)}）：
	 * <ul>
	 *   <li>{@code /**}：匹配所有路径</li>
	 *   <li>{@code /prefix/**}：匹配指定前缀及其所有子路径</li>
	 *   <li>{@code /prefix/*}：匹配指定前缀下的一级子路径</li>
	 *   <li>{@code /exact/path}：精确匹配</li>
	 * </ul>
	 *
	 * <p>多个过滤器按注册顺序依次执行。
	 *
	 * @param pathPattern Ant 风格路径模式，不能为 null 或空字符串；若不以 {@code /} 开头将自动补全
	 * @param filter      过滤器实例
	 * @return 当前路由器实例，支持链式调用
	 * @throws IllegalArgumentException 当 pathPattern 为空时抛出
	 */
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

	/**
	 * 注册一个全局过滤器，作用于所有请求。等价于 {@code filter("/**", filter)}。
	 *
	 * @param filter 过滤器实例
	 * @return 当前路由器实例，支持链式调用
	 */
	public HttpRouter filter(HttpFilter filter) {
		return filter("/**", filter);
	}

	/**
	 * 设置 404 兜底处理器。
	 *
	 * <p>当请求未匹配到任何路由，或路由命中但 Method 不匹配且该节点没有注册 allMethodHandler 时，
	 * 将回调此处理器。若未设置，则返回默认的 404 响应。
	 *
	 * @param handler 404 处理器
	 * @return 当前路由器实例，支持链式调用
	 */
	public HttpRouter notFound(RouteHandler handler) {
		this.notFoundHandler = handler;
		return this;
	}

	/**
	 * 设置全局异常处理器。
	 *
	 * <p>过滤器链或路由处理器抛出异常时，将回调此处理器。若未设置，则记录错误日志并返回 500 响应。
	 *
	 * @param handler 全局异常处理器
	 * @return 当前路由器实例，支持链式调用
	 */
	public HttpRouter error(ErrorHandler handler) {
		this.errorHandler = handler;
		return this;
	}

	/**
	 * 处理 HTTP 请求，按顺序执行：路径匹配 → 提取路径参数 → 匹配过滤器链 → 调用路由处理器。
	 *
	 * <p>执行流程：
	 * <ol>
	 *   <li>从 {@link RequestLine} 中获取请求 Method 与 Path</li>
	 *   <li>在前缀树中匹配路径，收集路径参数（{@code {name}} 形式的占位符）</li>
	 *   <li>未命中或 Method 不匹配：调用 {@link #notFoundHandler} 或返回默认 404</li>
	 *   <li>将路径参数写入 {@link HttpRequest#setAttribute(String, Serializable)}</li>
	 *   <li>按注册顺序收集路径模式命中的过滤器，组装 {@link HttpFilterChain}</li>
	 *   <li>执行过滤器链，异常时回调 {@link #errorHandler} 或返回默认 500</li>
	 * </ol>
	 *
	 * @param request HTTP 请求
	 * @return HTTP 响应
	 * @throws Exception 处理过程中透传的异常；若设置了 {@link #errorHandler} 则不会抛出
	 */
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
			} else {
				return getHttpResponse(request, HttpResponseStatus.C404);
			}
		}

		RouteHandler handler = matchNode.getHandler(method);
		if (handler == null) {
			handler = matchNode.allMethodHandler;
		}
		if (handler == null) {
			if (notFoundHandler != null) {
				return notFoundHandler.handle(request);
			} else {
				return getHttpResponse(request, HttpResponseStatus.C404);
			}
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
			} else {
				log.error("服务异常 {}", e.getMessage(), e);
				return getHttpResponse(request, HttpResponseStatus.C500);
			}
		}
	}

	/**
	 * 前缀树递归匹配，深度超过 {@link #MAX_ROUTE_DEPTH} 时抛异常防止 StackOverflow。
	 *
	 * <p>按优先级依次尝试：
	 * <ol>
	 *   <li>精确匹配（{@code children}）</li>
	 *   <li>参数匹配（{@code paramChildren}，将实际值写入 {@code params}）</li>
	 *   <li>通配符（{@code wildcardChild}）</li>
	 * </ol>
	 *
	 * <p>仅当匹配到的节点上注册了处理器（{@link TrieNode#hasHandler()}）时才视为匹配成功。
	 * 参数匹配的回溯在尝试下一个参数子节点前会清理 {@code params} 中写入的值，避免污染。
	 *
	 * @param node     当前前缀树节点
	 * @param segments 已切分的请求路径段
	 * @param index    当前匹配的段下标
	 * @param params   用于收集路径参数（{@code {name} -> value}）的输出参数
	 * @return 匹配到的终端节点；若未匹配到任何注册了处理器的节点则返回 {@code null}
	 * @throws IllegalStateException 当递归深度超过 {@link #MAX_ROUTE_DEPTH} 时抛出
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

	/**
	 * 构造一个带指定 HTTP 状态码的简单响应，body 使用状态码自带的默认内容。
	 *
	 * <p>用于 404、500 等默认错误响应的统一构造。
	 *
	 * @param request 当前请求，用于创建关联的响应对象
	 * @param status  HTTP 响应状态
	 * @return 构造好的 HTTP 响应
	 */
	private static HttpResponse getHttpResponse(HttpRequest request, HttpResponseStatus status) {
		HttpResponse response = new HttpResponse(request);
		response.setStatus(status);
		response.setBody(status.getHeaderBinary());
		return response;
	}

	/**
	 * 过滤器注册项：路径模式 + 过滤器实例。
	 *
	 * <p>路径模式在请求处理时通过 {@link PathUtils#matchPattern(String, String)} 进行匹配。
	 */
	private static class FilterMapping {
		/**
		 * Ant 风格路径模式，参考 {@link PathUtils#matchPattern(String, String)}。
		 */
		final String pathPattern;
		/**
		 * 关联的过滤器实例。
		 */
		final HttpFilter filter;

		FilterMapping(String pathPattern, HttpFilter filter) {
			this.pathPattern = pathPattern;
			this.filter = filter;
		}
	}
}
