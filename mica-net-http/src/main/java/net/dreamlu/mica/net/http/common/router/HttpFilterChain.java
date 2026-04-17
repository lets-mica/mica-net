package net.dreamlu.mica.net.http.common.router;

import net.dreamlu.mica.net.http.common.HttpRequest;
import net.dreamlu.mica.net.http.common.HttpResponse;

import java.util.List;

/**
 * 过滤器链
 *
 * @author L.cm
 */
public class HttpFilterChain {
	private final List<HttpFilter> filters;
	private final RouteHandler handler;
	private int index;

	HttpFilterChain(List<HttpFilter> filters, RouteHandler handler) {
		this.filters = filters;
		this.handler = handler;
		this.index = 0;
	}

	/**
	 * 执行下一个过滤器或最终路由处理器
	 *
	 * @param request HttpRequest
	 * @return HttpResponse
	 * @throws Exception Exception
	 */
	public HttpResponse doFilter(HttpRequest request) throws Exception {
		if (index < filters.size()) {
			return filters.get(index++).doFilter(request, this);
		}
		return handler.handle(request);
	}

}
