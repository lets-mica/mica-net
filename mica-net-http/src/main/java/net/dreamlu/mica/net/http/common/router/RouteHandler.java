package net.dreamlu.mica.net.http.common.router;

import net.dreamlu.mica.net.http.common.HttpRequest;
import net.dreamlu.mica.net.http.common.HttpResponse;

/**
 * 路由处理器
 *
 * @author L.cm
 */
@FunctionalInterface
public interface RouteHandler {

	/**
	 * 处理请求
	 *
	 * @param request HttpRequest
	 * @return HttpResponse
	 * @throws Exception Exception
	 */
	HttpResponse handle(HttpRequest request) throws Exception;

}
