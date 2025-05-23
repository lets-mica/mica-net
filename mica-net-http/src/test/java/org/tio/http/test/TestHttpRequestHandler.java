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
	public HttpResponse handler(HttpRequest packet) throws Exception {
		RequestLine requestLine = packet.getRequestLine();
		String path = requestLine.getPath();
		HttpResponse httpResponse = new HttpResponse(packet);
		if ("/sse".equals(path)) {
			SseEmitter emitter = SseEmitter.getEmitter(packet, httpResponse);
			// 跨域支持
			httpResponse.addHeader(HeaderName.Access_Control_Allow_Origin, HeaderValue.from("*"));
			new Thread(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < 10; i++) {
						SseEvent sseEvent = new SseEvent()
							.id(i)
							.event("message")
							.data("hello");
						emitter.push(sseEvent);
						ThreadUtils.sleep(1000);
					}
					emitter.close();
					System.out.println("emitter--------------close");
				}
			}).start();
			return httpResponse;
		}
		Collection<Cookie> cookies = packet.getCookies();
		for (Cookie cookie : cookies) {
			System.out.println(cookie.getName() + "\t" + cookie.getValue());
			httpResponse.addCookie(cookie);
		}
		System.out.println(packet.getCookieMap());

		httpResponse.setStatus(HttpResponseStatus.C200);
		byte[] body = packet.getBody();
		if (body != null) {
			System.out.println(new String(body));
		}

		httpResponse.setBody("hello".getBytes(StandardCharsets.UTF_8));
		return httpResponse;
	}

	@Override
	public HttpResponse resp404(HttpRequest request, RequestLine requestLine) throws Exception {
		HttpResponse httpResponse = new HttpResponse(request);
		httpResponse.setStatus(HttpResponseStatus.C404);
		return httpResponse;
	}

	@Override
	public HttpResponse resp500(HttpRequest request, RequestLine requestLine, Throwable throwable) throws Exception {
		HttpResponse httpResponse = new HttpResponse(request);
		httpResponse.setStatus(HttpResponseStatus.C500);
		httpResponse.setBody(throwable.getMessage().getBytes(StandardCharsets.UTF_8));
		return httpResponse;
	}

}
