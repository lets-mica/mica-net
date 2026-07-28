/*
	Apache License
	Version 2.0, January 2004
	http://www.apache.org/licenses/
*/
package net.dreamlu.mica.net.core.udp;

import net.dreamlu.mica.net.client.udp.UdpClient;
import net.dreamlu.mica.net.client.udp.UdpClientConfig;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.core.intf.UdpChannel;
import net.dreamlu.mica.net.core.intf.UdpHandler;
import net.dreamlu.mica.net.server.udp.UdpServer;
import net.dreamlu.mica.net.server.udp.UdpServerConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NIO UDP 行为回归：回包、一包多帧、关闭后不可再发、peer 清理。
 */
class UdpNioTest {

	@Test
	void echoRoundTrip() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		List<String> received = new CopyOnWriteArrayList<>();
		int port = findFreeUdpPort();

		UdpHandler serverHandler = textHandler((msg, channel) ->
			channel.send(new TextPacket("echo:" + msg)));
		UdpServer server = new UdpServer(
			UdpServerConfig.builder().port(port).build(),
			serverHandler
		);
		server.start();
		try {
			UdpHandler clientHandler = textHandler((msg, channel) -> {
				received.add(msg);
				latch.countDown();
			});
			UdpClient client = new UdpClient(
				UdpClientConfig.builder().host("127.0.0.1").port(port).build(),
				clientHandler
			);
			client.start();
			try {
				Assertions.assertTrue(client.send(new TextPacket("hello")));
				Assertions.assertTrue(latch.await(3, TimeUnit.SECONDS),
					"expected echo, got=" + received);
				Assertions.assertEquals(Collections.singletonList("echo:hello"), received);
			} finally {
				client.close();
			}
		} finally {
			server.close();
		}
		Assertions.assertEquals(0, server.getPeerChannelCount());
	}

	@Test
	void decodeMultipleFramesInOneDatagram() throws Exception {
		CountDownLatch latch = new CountDownLatch(2);
		List<String> received = new CopyOnWriteArrayList<>();
		int port = findFreeUdpPort();

		UdpHandler serverHandler = consumingTextHandler((msg, channel) -> {
			received.add(msg);
			latch.countDown();
		});
		UdpServer server = new UdpServer(
			UdpServerConfig.builder().port(port).build(),
			serverHandler
		);
		server.start();
		try (DatagramSocket socket = new DatagramSocket()) {
			ByteBuffer payload = ByteBuffer.allocate(64);
			putFrame(payload, "a");
			putFrame(payload, "b");
			payload.flip();
			byte[] bytes = new byte[payload.remaining()];
			payload.get(bytes);
			socket.send(new DatagramPacket(bytes, bytes.length,
				InetAddress.getByName("127.0.0.1"), port));
			Assertions.assertTrue(latch.await(3, TimeUnit.SECONDS),
				"expected 2 frames, got=" + received);
			Assertions.assertEquals(2, received.size());
			Assertions.assertTrue(received.contains("a"));
			Assertions.assertTrue(received.contains("b"));
		} finally {
			server.close();
		}
	}

	@Test
	void clientSendReturnsFalseAfterClose() throws Exception {
		int port = findFreeUdpPort();
		UdpServer server = new UdpServer(
			UdpServerConfig.builder().port(port).build(),
			textHandler((msg, channel) -> {
			})
		);
		server.start();
		try {
			UdpClient client = new UdpClient(
				UdpClientConfig.builder()
					.host("127.0.0.1")
					.port(port)
					.sendQueueCapacity(16)
					.build(),
				textHandler((msg, channel) -> {
				})
			);
			client.start();
			client.close();
			Assertions.assertFalse(client.send(new TextPacket("after-close")));
		} finally {
			server.close();
		}
	}

	@Test
	void sendQueueCapacityMustBePositive() {
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> UdpClientConfig.builder()
				.host("127.0.0.1")
				.port(1)
				.sendQueueCapacity(0)
				.build());
	}

	@Test
	void maxPeersMustBePositive() {
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> UdpServerConfig.builder().port(1).maxPeers(0).build());
	}

	@Test
	void maxPeersRejectsNewPeerWhenFull() throws Exception {
		CountDownLatch firstHandled = new CountDownLatch(1);
		CountDownLatch secondHandled = new CountDownLatch(1);
		int port = findFreeUdpPort();
		UdpServer server = new UdpServer(
			UdpServerConfig.builder()
				.port(port)
				.maxPeers(1)
				.peerIdleTimeoutMs(0)
				.build(),
			consumingTextHandler((msg, channel) -> {
				if ("first".equals(msg)) {
					firstHandled.countDown();
				} else {
					secondHandled.countDown();
				}
			})
		);
		server.start();
		try (DatagramSocket a = new DatagramSocket();
			 DatagramSocket b = new DatagramSocket()) {
			sendRaw(a, port, "first");
			Assertions.assertTrue(firstHandled.await(3, TimeUnit.SECONDS));
			Assertions.assertEquals(1, server.getPeerChannelCount());

			sendRaw(b, port, "second");
			Assertions.assertFalse(secondHandled.await(300, TimeUnit.MILLISECONDS),
				"second peer should be rejected when maxPeers=1");
			Assertions.assertEquals(1, server.getPeerChannelCount());
		} finally {
			server.close();
		}
	}

	@Test
	void samePeerHandlersRunSerially() throws Exception {
		final int n = 8;
		AtomicInteger inFlight = new AtomicInteger();
		AtomicInteger maxInFlight = new AtomicInteger();
		CountDownLatch done = new CountDownLatch(n);
		int port = findFreeUdpPort();

		UdpServer server = new UdpServer(
			UdpServerConfig.builder().port(port).workerThreads(4).build(),
			consumingTextHandler((msg, channel) -> {
				int cur = inFlight.incrementAndGet();
				maxInFlight.accumulateAndGet(cur, Math::max);
				try {
					Thread.sleep(15);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					inFlight.decrementAndGet();
					done.countDown();
				}
			})
		);
		server.start();
		try (DatagramSocket socket = new DatagramSocket()) {
			for (int i = 0; i < n; i++) {
				sendRaw(socket, port, "m" + i);
			}
			Assertions.assertTrue(done.await(5, TimeUnit.SECONDS));
			Assertions.assertEquals(1, maxInFlight.get(),
				"same peer handlers must not overlap");
		} finally {
			server.close();
		}
	}

	private static void sendRaw(DatagramSocket socket, int port, String text) throws Exception {
		ByteBuffer payload = ByteBuffer.allocate(64);
		putFrame(payload, text);
		payload.flip();
		byte[] bytes = new byte[payload.remaining()];
		payload.get(bytes);
		socket.send(new DatagramPacket(bytes, bytes.length,
			InetAddress.getByName("127.0.0.1"), port));
	}

	private static int findFreeUdpPort() throws Exception {
		try (DatagramSocket socket = new DatagramSocket(0)) {
			return socket.getLocalPort();
		}
	}

	private static void putFrame(ByteBuffer buffer, String text) {
		byte[] body = text.getBytes(StandardCharsets.UTF_8);
		buffer.putShort((short) body.length);
		buffer.put(body);
	}

	/**
	 * 绝对读、不推进 position——单帧场景可用；多帧依赖框架检测无进度后退出。
	 */
	private static UdpHandler textHandler(BiHandler bi) {
		return new UdpHandler() {
			@Override
			public Packet decode(ByteBuffer buffer, int limit, int position,
								 int readableLength, UdpChannel ctx) {
				if (readableLength < 2) {
					return null;
				}
				int len = ((buffer.get(position) & 0xff) << 8) | (buffer.get(position + 1) & 0xff);
				if (readableLength < 2 + len) {
					return null;
				}
				byte[] body = new byte[len];
				for (int i = 0; i < len; i++) {
					body[i] = buffer.get(position + 2 + i);
				}
				return new TextPacket(new String(body, StandardCharsets.UTF_8));
			}

			@Override
			public ByteBuffer encode(Packet packet, UdpConfig config, UdpChannel ctx) {
				return encodeFrame(((TextPacket) packet).getBody());
			}

			@Override
			public void handler(Packet packet, UdpChannel channel) {
				bi.accept(((TextPacket) packet).getBody(), channel);
			}
		};
	}

	/**
	 * 相对读并推进 position，用于验证一包多帧 decode 循环。
	 */
	private static UdpHandler consumingTextHandler(BiHandler bi) {
		return new UdpHandler() {
			@Override
			public Packet decode(ByteBuffer buffer, int limit, int position,
								 int readableLength, UdpChannel ctx) {
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

			@Override
			public ByteBuffer encode(Packet packet, UdpConfig config, UdpChannel ctx) {
				return encodeFrame(((TextPacket) packet).getBody());
			}

			@Override
			public void handler(Packet packet, UdpChannel channel) {
				bi.accept(((TextPacket) packet).getBody(), channel);
			}
		};
	}

	private static ByteBuffer encodeFrame(String text) {
		byte[] body = text.getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.allocate(2 + body.length);
		buffer.putShort((short) body.length);
		buffer.put(body);
		buffer.flip();
		return buffer;
	}

	@FunctionalInterface
	private interface BiHandler {
		void accept(String msg, UdpChannel channel);
	}

	static final class TextPacket extends Packet {
		private static final long serialVersionUID = 1L;
		private final String body;

		TextPacket(String body) {
			this.body = body;
		}

		String getBody() {
			return body;
		}

		@Override
		public String logstr() {
			return "TextPacket(" + body + ")";
		}
	}
}
