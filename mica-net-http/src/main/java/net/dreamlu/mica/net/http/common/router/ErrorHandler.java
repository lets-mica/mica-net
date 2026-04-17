package net.dreamlu.mica.net.http.common.router;

import net.dreamlu.mica.net.http.common.HttpRequest;
import net.dreamlu.mica.net.http.common.HttpResponse;

/**
 * 异常处理器
 *
 * @author L.cm
 */
@FunctionalInterface
public interface ErrorHandler {

	/**
	 * 处理异常
	 *
	 * @param request HttpRequest
	 * @param error   异常
	 * @return HttpResponse
	 */
	HttpResponse handle(HttpRequest request, Throwable error);

}
