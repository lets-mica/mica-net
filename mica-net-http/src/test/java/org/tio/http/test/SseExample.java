package org.tio.http.test;

import org.tio.http.common.HeaderName;
import org.tio.http.common.HeaderValue;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.http.common.HttpResponseStatus;
import org.tio.http.common.handler.HttpRequestHandler;
import org.tio.http.common.stream.HttpStream;
import org.tio.http.server.HttpServerStarter;
import org.tio.utils.thread.ThreadUtils;

/**
 * SSE (Server-Sent Events) 示例
 * <p>
 * 演示如何使用 HttpStream 实现服务端推送事件
 *
 * @author L.cm
 */
public class SseExample implements HttpRequestHandler {

	@Override
	public HttpResponse handler(HttpRequest request) throws Exception {
		String path = request.getRequestLine().getPath();

		if ("/sse/counter".equals(path)) {
			return handleCounterSse(request);
		} else if ("/sse/notification".equals(path)) {
			return handleNotificationSse(request);
		} else if ("/sse/messages".equals(path)) {
			return handleMessagesSse(request);
		}

		// 默认返回
		HttpResponse response = new HttpResponse(request);
		response.setStatus(HttpResponseStatus.C200);
		response.setBody("Try /sse/counter, /sse/notification, or /sse/messages".getBytes());
		return response;
	}

	/**
	 * 计数器 SSE 示例
	 * <p>
	 * 每秒发送一个递增的计数器
	 */
	private HttpResponse handleCounterSse(HttpRequest request) {
		HttpResponse response = new HttpResponse(request);
		response.addHeader(HeaderName.Access_Control_Allow_Origin, HeaderValue.from("*"));
		// 开启 SSE 流
		HttpStream stream = response.startSse(request);

		// 在异步线程中发送事件
		new Thread(() -> {
			for (int i = 0; i < 10; i++) {
				stream.send(i, "counter", "Count: " + i);
				ThreadUtils.sleep(1000);
			}
			stream.end();
		}).start();

		return response;
	}

	/**
	 * 通知推送 SSE 示例
	 * <p>
	 * 模拟推送不同类型的通知消息
	 */
	private HttpResponse handleNotificationSse(HttpRequest request) {
		HttpResponse response = new HttpResponse(request);
		response.addHeader(HeaderName.Access_Control_Allow_Origin, HeaderValue.from("*"));
		HttpStream stream = response.startSse(request);

		new Thread(() -> {
			String[] types = {"info", "success", "warning", "error"};
			for (int i = 0; i < 8; i++) {
				String type = types[i % types.length];
				String message = "Notification " + i + " [" + type + "]: " + System.currentTimeMillis();
				stream.send(type, message);
				ThreadUtils.sleep(2000);
			}
			stream.end();
		}).start();

		return response;
	}

	/**
	 * 多行数据 SSE 示例
	 * <p>
	 * 演示如何发送多行 data 字段
	 */
	private HttpResponse handleMessagesSse(HttpRequest request) {
		HttpResponse response = new HttpResponse(request);
		response.addHeader(HeaderName.Access_Control_Allow_Origin, HeaderValue.from("*"));
		HttpStream stream = response.startSse(request);

		new Thread(() -> {
			// 发送多行数据的消息
			String multiLineData = "Line 1\nLine 2\nLine 3";
			stream.send("message", multiLineData);

			// 发送包含换行符的字符串（自动处理 data: 前缀）
			stream.send("message", "First line\nSecond line\nThird line");

			// 发送普通对象
			stream.send("data", new Object() {
				@Override
				public String toString() {
					return "{ \"time\": " + System.currentTimeMillis() + " }";
				}
			});

			stream.end();
		}).start();

		return response;
	}

	public static void main(String[] args) throws Exception {
		SseExample handler = new SseExample();
		HttpServerStarter starter = new HttpServerStarter(8080, handler);
		starter.start();
		System.out.println("SSE Server started on http://localhost:8080");
		System.out.println("Try (in browser or using curl):");
		System.out.println("  curl -vN http://localhost:8080/sse/counter");
		System.out.println("  curl -vN http://localhost:8080/sse/notification");
		System.out.println("  curl -vN http://localhost:8080/sse/messages");
	}
}
