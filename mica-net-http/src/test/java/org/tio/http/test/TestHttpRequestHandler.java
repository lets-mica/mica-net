package org.tio.http.test;

import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.http.common.*;
import org.tio.http.common.handler.HttpRequestHandler;
import org.tio.http.common.sse.SSEPacket;
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
			httpResponse.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.TEXT_EVENT_STREAM);
			httpResponse.addHeader(HeaderName.Connection, HeaderValue.Connection.keep_alive);
			httpResponse.addHeader(HeaderName.Cache_Control, HeaderValue.Cache_Control.no_cache);
			httpResponse.addHeader(HeaderName.Access_Control_Allow_Origin, HeaderValue.from("*"));

			new Thread(new Runnable() {
				@Override
				public void run() {
					while (true) {
						ChannelContext context = packet.getChannelContext();
						SSEPacket ssePacket = new SSEPacket.Builder()
							.event("message")
							.data("hello".getBytes(StandardCharsets.UTF_8))
							.build();
						Tio.send(context, ssePacket);
						ThreadUtils.sleep(1000);
					}
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
