package net.dreamlu.mica.net.http.common.router;

import java.util.Map;

/**
 * 路由匹配结果
 *
 * @author L.cm
 */
class RouteResult {
	final RouteHandler handler;
	final Map<String, String> pathParams;

	RouteResult(RouteHandler handler, Map<String, String> pathParams) {
		this.handler = handler;
		this.pathParams = pathParams;
	}
}
