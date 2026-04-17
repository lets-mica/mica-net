package net.dreamlu.mica.net.http.common.router;

import net.dreamlu.mica.net.http.common.HttpRequest;
import net.dreamlu.mica.net.http.common.HttpResponse;

/**
 * HTTP 过滤器
 *
 * @author L.cm
 */
public interface HttpFilter {

	/**
	 * 过滤器执行
	 *
	 * @param request HttpRequest
	 * @param chain   HttpFilterChain
	 * @return HttpResponse
	 * @throws Exception Exception
	 */
	HttpResponse doFilter(HttpRequest request, HttpFilterChain chain) throws Exception;

}
