package net.dreamlu.mica.net.core.udp;

import net.dreamlu.mica.net.client.udp.UdpClient;
import net.dreamlu.mica.net.client.udp.UdpClientConfig;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.core.intf.UdpChannel;
import net.dreamlu.mica.net.core.intf.UdpHandler;
import net.dreamlu.mica.net.server.udp.UdpServer;
import net.dreamlu.mica.net.server.udp.UdpServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 标准 NIO UDP Demo 入口，演示 {@link UdpHandler} / {@link UdpChannel} 的使用方式。
 * <p>
 * 自包含运行：同一 JVM 内启动 server 与 client，交换若干报文后退出。
 * 也可以通过 {@code java UdpDemo server|client} 单独运行某一端。
 * <p>
 * 协议：2 字节大端长度前缀 + UTF-8 文本负载（演示用），结构与 TCP TioHandler
 * 的三段式（decode/encode/handler）一致，但不依赖 {@code ChannelContext}。
 * <p>
 * 注入自定义线程池（如 mica-net 的 {@code ThreadUtils.getGroupExecutor()}）示例：
 * <pre>{@code
 * ExecutorService sharedPool = ThreadUtils.getGroupExecutor();
 * UdpServerConfig cfg = UdpServerConfig.builder()
 *     .port(9999)
 *     .workerPool(sharedPool)
 *     .build();
 * }</pre>
 *
 * @author L.cm
 */
public class UdpDemo {
	private static final Logger log = LoggerFactory.getLogger(UdpDemo.class);
	private static final int PORT = 9999;

	public static void main(String[] args) throws Exception {
		String mode = args.length > 0 ? args[0].toLowerCase() : "both";
		switch (mode) {
			case "server":
				startServer();
				break;
			case "client":
				startClient();
				break;
			case "both":
			default:
				runBoth();
				break;
		}
	}

	private static void runBoth() throws Exception {
		AtomicInteger counter = new AtomicInteger();
		UdpHandler serverHandler = new UdpHandler() {
			@Override
			public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, UdpChannel ctx) {
				return Codec.decode(buffer, readableLength, position);
			}

			@Override
			public ByteBuffer encode(Packet packet, UdpConfig config, UdpChannel ctx) {
				return Codec.encode(((TextPacket) packet).getBody());
			}

			@Override
			public void handler(Packet packet, UdpChannel channel) {
				String msg = ((TextPacket) packet).getBody();
				log.info("[server] recv from {} -> {}", channel.remoteAddress(), msg);
				String echo = "echo-" + counter.incrementAndGet() + ":" + msg;
				channel.send(new TextPacket(echo));
			}
		};
		UdpServer server = new UdpServer(
			UdpServerConfig.builder().port(PORT).build(),
			serverHandler
		);
		server.start();
		startClient();
	}

	private static void startServer() {
		AtomicInteger counter = new AtomicInteger();
		UdpHandler handler = new UdpHandler() {
			@Override
			public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, UdpChannel ctx) {
				return Codec.decode(buffer, readableLength, position);
			}

			@Override
			public ByteBuffer encode(Packet packet, UdpConfig config, UdpChannel ctx) {
				return Codec.encode(((TextPacket) packet).getBody());
			}

			@Override
			public void handler(Packet packet, UdpChannel channel) {
				String msg = ((TextPacket) packet).getBody();
				log.info("[server] recv from {} -> {}", channel.remoteAddress(), msg);
				String echo = "echo-" + counter.incrementAndGet() + ":" + msg;
				channel.send(new TextPacket(echo));
			}
		};
		UdpServer server = new UdpServer(
			UdpServerConfig.builder().port(PORT).build(),
			handler
		);
		try {
			server.start();
		} catch (IOException e) {
			log.error("server start failed", e);
			return;
		}
		Runtime.getRuntime().addShutdownHook(new Thread(server::close));
	}

	private static void startClient() throws Exception {
		CountDownLatch latch = new CountDownLatch(3);
		UdpHandler handler = new UdpHandler() {
			@Override
			public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, UdpChannel ctx) {
				return Codec.decode(buffer, readableLength, position);
			}

			@Override
			public ByteBuffer encode(Packet packet, UdpConfig config, UdpChannel ctx) {
				return Codec.encode(((TextPacket) packet).getBody());
			}

			@Override
			public void handler(Packet packet, UdpChannel channel) {
				log.info("[client] recv -> {}", ((TextPacket) packet).getBody());
				latch.countDown();
			}
		};
		UdpClient client = new UdpClient(
			UdpClientConfig.builder().host("127.0.0.1").port(PORT).build(),
			handler
		);
		client.start();
		for (int i = 0; i < 100; i++) {
			String payload = "hello-" + i + '-' + System.currentTimeMillis();
			log.info("[client] send -> {}", payload);
			client.send(new TextPacket(payload));
			Thread.sleep(500);
		}
		if (!latch.await(5, TimeUnit.SECONDS)) {
			log.warn("client not received all echo");
		}
		client.close();
	}

	/**
	 * 长度前缀编解码工具：decode 成功后推进 position，配合框架 datagram 内多帧循环。
	 */
	static final class Codec {
		private Codec() {
		}

		static ByteBuffer encode(String text) {
			byte[] body = text.getBytes(StandardCharsets.UTF_8);
			ByteBuffer buffer = ByteBuffer.allocate(2 + body.length);
			buffer.putShort((short) body.length);
			buffer.put(body);
			buffer.flip();
			return buffer;
		}

		static Packet decode(ByteBuffer buffer, int readableLength, int position) {
			if (readableLength < 2) {
				return null;
			}
			int len = buffer.getShort(position) & 0xffff;
			if (readableLength < 2 + len) {
				return null;
			}
			buffer.position(position + 2);
			byte[] body = new byte[len];
			buffer.get(body);
			return new TextPacket(new String(body, StandardCharsets.UTF_8));
		}
	}

	/**
	 * 业务包：仅持有文本负载，与 mica-net 的 Packet 体系保持一致。
	 */
	static final class TextPacket extends Packet {
		private static final long serialVersionUID = 1L;
		private final String body;

		TextPacket(String body) {
			this.body = body;
		}

		public String getBody() {
			return body;
		}

		@Override
		public String logstr() {
			return "TextPacket(" + body + ")";
		}
	}
}
