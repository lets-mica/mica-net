package org.tio.http.test;

import org.tio.core.ChannelContext;
import org.tio.core.intf.Packet;
import org.tio.core.intf.PacketListener;
import org.tio.http.common.HeaderName;
import org.tio.http.common.HeaderValue;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.http.common.HttpResponseStatus;
import org.tio.http.common.handler.HttpRequestHandler;
import org.tio.http.common.stream.HttpStream;
import org.tio.http.server.HttpServerStarter;
import org.tio.utils.thread.ThreadUtils;

import java.io.IOException;

/**
 * HTTP Streaming 示例
 * <p>
 * 演示如何使用 HttpStream 实现 HTTP 分块传输（Transfer-Encoding: chunked）
 *
 * @author L.cm
 */
public class StreamExample implements HttpRequestHandler {

	@Override
	public HttpResponse handler(HttpRequest request) throws Exception {
		String path = request.getRequestLine().getPath();

		if ("/stream".equals(path)) {
			return handleStream(request);
		} else if ("/stream/file".equals(path)) {
			return handleFileStream(request);
		}

		// 默认返回
		HttpResponse response = new HttpResponse(request);
		response.setStatus(HttpResponseStatus.C200);
		response.setBody("Try /stream or /stream/file".getBytes());
		return response;
	}

	/**
	 * 简单的流式响应示例
	 * <p>
	 * 发送多块数据，每块间隔1秒
	 */
	private HttpResponse handleStream(HttpRequest request) {
		HttpResponse response = new HttpResponse(request);
		response.setStatus(HttpResponseStatus.C200);
		response.addHeader(HeaderName.Content_Type, HeaderValue.from("text/plain; charset=utf-8"));
		// 开启流式响应
		HttpStream out = response.startStream(request);

		response.setPacketListener(new PacketListener() {
			@Override
			public void onAfterSent(ChannelContext context, Packet packet, boolean isSentSuccess) throws Exception {
				// 在异步线程中发送数据
				new Thread(() -> {
					try {
						for (int i = 1; i <= 5; i++) {
							String chunk = "Chunk " + i + " - " + System.currentTimeMillis() + "\n";
							out.write(chunk.getBytes());
							ThreadUtils.sleep(1000);
						}
						// 结束流
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}).start();
			}
		});

		return response;
	}

	/**
	 * 文件流式传输示例
	 * <p>
	 * 模拟大文件分块传输
	 */
	private HttpResponse handleFileStream(HttpRequest request) {
		HttpResponse response = new HttpResponse(request);
		response.setStatus(HttpResponseStatus.C200);
		response.addHeader(HeaderName.Content_Type, HeaderValue.from("application/octet-stream"));
		response.addHeader(HeaderName.Content_Disposition, HeaderValue.from("attachment; filename=\"largefile.bin\""));
		// 开启流式响应
		HttpStream out = response.startStream(request);

		response.setPacketListener(new PacketListener() {
			@Override
			public void onAfterSent(ChannelContext context, Packet packet, boolean isSentSuccess) throws Exception {
				// 在异步线程中模拟文件传输
				new Thread(() -> {
					try {
						// 模拟分块发送大文件
						byte[] buffer = new byte[1024]; // 1KB per chunk
						for (int i = 0; i < 100; i++) {
							// 填充模拟数据
							for (int j = 0; j < buffer.length; j++) {
								buffer[j] = (byte) (i % 256);
							}
							out.write(buffer);
							ThreadUtils.sleep(100); // 模拟IO延迟
						}
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}).start();
			}
		});

		return response;
	}

	public static void main(String[] args) throws Exception {
		StreamExample handler = new StreamExample();
		HttpServerStarter starter = new HttpServerStarter(8080, handler);
		starter.start();
		System.out.println("HTTP Streaming Server started on http://localhost:8080");
		System.out.println("Try:");
		System.out.println("  curl -vN http://localhost:8080/stream");
		System.out.println("  curl -vN http://localhost:8080/stream/file");
	}
}
