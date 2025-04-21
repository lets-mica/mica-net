package org.tio.http.test;

import org.tio.http.common.*;
import org.tio.http.common.handler.HttpRequestHandler;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class TestHttpRequestHandler implements HttpRequestHandler {

	@Override
	public HttpResponse handler(HttpRequest packet) throws Exception {
		HttpResponse httpResponse = new HttpResponse(packet);
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
