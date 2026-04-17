package net.dreamlu.mica.net.http.common.router;

import net.dreamlu.mica.net.http.common.Method;
import net.dreamlu.mica.net.http.common.handler.HttpRequestHandler;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 前缀树节点
 *
 * @author L.cm
 */
class TrieNode {
	final Map<String, TrieNode> children = new HashMap<>();
	/**
	 * 参数子节点 Map，key 为参数名（如 "id"、"userId"）
	 */
	final Map<String, TrieNode> paramChildren = new HashMap<>();
	TrieNode wildcardChild;
	/**
	 * 该节点绑定的处理器，按 HTTP Method 分组；null 表示匹配所有 Method
	 */
	final EnumMap<Method, RouteHandler> handlers = new EnumMap<>(Method.class);
	RouteHandler allMethodHandler;

	void addHandler(Method method, RouteHandler handler) {
		if (method == null) {
			if (allMethodHandler != null) {
				throw new IllegalArgumentException("Duplicate route for all methods");
			}
			allMethodHandler = handler;
		} else {
			if (handlers.containsKey(method)) {
				throw new IllegalArgumentException("Duplicate route for " + method);
			}
			handlers.put(method, handler);
		}
	}

	RouteHandler getHandler(Method method) {
		RouteHandler handler = handlers.get(method);
		if (handler != null) {
			return handler;
		}
		return allMethodHandler;
	}

	boolean hasHandler() {
		return allMethodHandler != null || !handlers.isEmpty();
	}
}
