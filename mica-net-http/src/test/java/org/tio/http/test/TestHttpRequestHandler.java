package org.tio.http.test;

import org.tio.http.common.*;
import org.tio.http.common.handler.HttpRequestHandler;
import org.tio.http.common.sse.SseEvent;
import org.tio.http.common.sse.SseEmitter;
import org.tio.utils.thread.ThreadUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class TestHttpRequestHandler implements HttpRequestHandler {

	@Override
	public HttpResponse handler(HttpRequest request) throws Exception {
		RequestLine requestLine = request.getRequestLine();
		String path = requestLine.getPath();
		HttpResponse httpResponse = new HttpResponse(request);
		if ("/sse".equals(path)) {
			SseEmitter emitter = SseEmitter.getEmitter(request, httpResponse);
			// 跨域支持
			httpResponse.addHeader(HeaderName.Access_Control_Allow_Origin, HeaderValue.from("*"));
			new Thread(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < 10; i++) {
						SseEvent sseEvent = new SseEvent()
							.id(i)
							.name("message")
							.data("hello\n123123");
						emitter.send(sseEvent);
						ThreadUtils.sleep(1000);
					}
					emitter.close();
					System.out.println("emitter--------------close");
				}
			}).start();
			return httpResponse;
		}
		Collection<Cookie> cookies = request.getCookies();
		for (Cookie cookie : cookies) {
			System.out.println(cookie.getName() + "\t" + cookie.getValue());
			httpResponse.addCookie(cookie);
		}
		System.out.println(request.getCookieMap());

		httpResponse.setStatus(HttpResponseStatus.C200);
		byte[] body = request.getBody();
		if (body != null) {
			System.out.println(new String(body));
		}

		httpResponse.setBody("hello".getBytes(StandardCharsets.UTF_8));
		return httpResponse;
	}

}
