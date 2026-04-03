package net.dreamlu.mica.net.http.test;

import net.dreamlu.mica.net.http.common.*;
import net.dreamlu.mica.net.http.common.handler.HttpRequestHandler;
import net.dreamlu.mica.net.http.common.stream.HttpStream;
import net.dreamlu.mica.net.utils.thread.ThreadUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class TestHttpRequestHandler implements HttpRequestHandler {

	@Override
	public HttpResponse handler(HttpRequest request) throws Exception {
		RequestLine requestLine = request.getRequestLine();
		String path = requestLine.getPath();
		HttpResponse httpResponse = new HttpResponse(request);
		if ("/sse".equals(path)) {
			HttpStream stream = httpResponse.startSse(request);
			// 跨域支持
			httpResponse.addHeader(HeaderName.Access_Control_Allow_Origin, HeaderValue.from("*"));
			new Thread(() -> {
				for (int i = 0; i < 10; i++) {
					stream.send(i, "message", "hello\n123123");
					ThreadUtils.sleep(1000);
				}
				stream.close();
				System.out.println("stream--------------end");
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
