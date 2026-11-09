/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & dreamlu.net).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dreamlu.mica.net.http.test.ws;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.websocket.common.Ws;
import net.dreamlu.mica.net.websocket.common.WsRequest;
import net.dreamlu.mica.net.websocket.common.WsSessionContext;
import net.dreamlu.mica.net.websocket.server.WsServerStarter;
import net.dreamlu.mica.net.websocket.server.handler.IWsMsgHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WebSocket 端到端测试，用裸 socket 模拟客户端，覆盖握手、收发帧、关闭握手。
 *
 * @author L.cm
 */
class WsEndToEndTest {

	private WsServerStarter starter;
	private int port;
	/**
	 * 业务侧拿到的连接上下文，用于测试服务端主动关闭
	 */
	private final AtomicReference<ChannelContext> contextRef = new AtomicReference<>();

	@BeforeEach
	void setUp() throws Exception {
		port = findFreePort();
		IWsMsgHandler handler = new IWsMsgHandler() {
			@Override
			public Object onText(WsRequest request, String text, ChannelContext context) {
				contextRef.set(context);
				return "echo:" + text;
			}

			@Override
			public Object onBytes(WsRequest request, byte[] bytes, ChannelContext context) {
				contextRef.set(context);
				return bytes;
			}
		};
		starter = new WsServerStarter(port, handler);
		starter.start();
	}

	@AfterEach
	void tearDown() {
		if (starter != null) {
			starter.stop();
		}
		contextRef.set(null);
	}

	@Test
	void handshake_returns101WithAcceptKey() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();

			assertEquals(101, client.getStatusCode());
			// RFC 6455 §1.3 的示例值
			assertEquals("s3pPLMBiTxaQ9kYGzzhZRbK+xOo=", client.getHeader("Sec-WebSocket-Accept"));
			// 头值大小写不敏感
			assertTrue(client.getHeader("Upgrade").equalsIgnoreCase("websocket"));
			assertTrue(client.getHeader("Connection").equalsIgnoreCase("upgrade"));
		}
	}

	@Test
	void handshake_missingVersion_returns426() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake("GET /ws HTTP/1.1", "Upgrade: websocket\r\nConnection: Upgrade\r\n"
				+ "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n");

			assertEquals(426, client.getStatusCode());
			assertEquals("13", client.getHeader("Sec-WebSocket-Version"));
		}
	}

	@Test
	void handshake_missingKey_returns400() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake("GET /ws HTTP/1.1", "Upgrade: websocket\r\nConnection: Upgrade\r\n"
				+ "Sec-WebSocket-Version: 13\r\n");

			assertEquals(400, client.getStatusCode());
		}
	}

	@Test
	void handshake_postMethod_returns405() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake("POST /ws HTTP/1.1", "Upgrade: websocket\r\nConnection: Upgrade\r\n"
				+ "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\nSec-WebSocket-Version: 13\r\n");

			assertEquals(405, client.getStatusCode());
		}
	}

	@Test
	void handshake_missingUpgradeHeader_returns400() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake("GET /ws HTTP/1.1", "Connection: Upgrade\r\n"
				+ "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\nSec-WebSocket-Version: 13\r\n");

			assertEquals(400, client.getStatusCode());
		}
	}

	@Test
	void textMessage_isEchoed() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();
			client.sendText("hello");

			WsTestClient.Frame frame = client.readFrame();

			assertNotNull(frame);
			assertEquals(0x1, frame.opcode);
			assertEquals("echo:hello", frame.text());
		}
	}

	@Test
	void binaryMessage_isEchoed() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();
			byte[] payload = {1, 2, 3, 4, 5};
			client.sendFrame(0x2, payload, true, true);

			WsTestClient.Frame frame = client.readFrame();

			assertNotNull(frame);
			assertEquals(0x2, frame.opcode);
			assertArrayEquals(payload, frame.payload);
		}
	}

	@Test
	void ping_isAnsweredWithPong() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();
			byte[] data = "ping-data".getBytes(StandardCharsets.UTF_8);
			client.sendPing(data);

			WsTestClient.Frame frame = client.readFrame();

			assertNotNull(frame);
			// RFC 6455 §5.5.3：收到 Ping 必须回 Pong，负载原样带回
			assertEquals(0xA, frame.opcode);
			assertArrayEquals(data, frame.payload);
		}
	}

	@Test
	void fragmentedTextMessage_isReassembled() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();
			client.sendFrame(0x1, "hello ".getBytes(StandardCharsets.UTF_8), false, true);
			client.sendFrame(0x0, "world".getBytes(StandardCharsets.UTF_8), true, true);

			WsTestClient.Frame frame = client.readFrame();

			assertNotNull(frame);
			assertEquals(0x1, frame.opcode);
			assertEquals("echo:hello world", frame.text());
		}
	}

	@Test
	void emptyTextMessage_isDeliveredToHandler() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();
			client.sendFrame(0x1, new byte[0], true, true);

			WsTestClient.Frame frame = client.readFrame();

			assertNotNull(frame);
			assertEquals(0x1, frame.opcode);
			assertEquals("echo:", frame.text());
		}
	}

	@Test
	void closeFrame_isEchoedThenConnectionClosed() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();
			client.sendClose(1000, "bye");

			WsTestClient.Frame frame = client.readFrame();

			assertNotNull(frame);
			// RFC 6455 §5.5.1：收到 Close 必须回一个 Close，状态码保持一致
			assertEquals(0x8, frame.opcode);
			assertEquals(0x03, frame.payload[0] & 0xFF);
			assertEquals(0xE8, frame.payload[1] & 0xFF);
			assertTrue(waitForServerClose(client));
		}
	}

	@Test
	void serverInitiatedClose_sendsCloseFrame() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();
			client.sendText("hello");
			// 先把回包读掉
			assertNotNull(client.readFrame());

			ChannelContext context = contextRef.get();
			assertNotNull(context);
			assertTrue(Ws.close(context, 1000, "bye"));

			WsTestClient.Frame frame = client.readFrame();

			assertNotNull(frame);
			assertEquals(0x8, frame.opcode);
			assertEquals(0x03, frame.payload[0] & 0xFF);
			assertEquals(0xE8, frame.payload[1] & 0xFF);
		}
	}

	/**
	 * RFC 6455 §5.5.1：本端发出 Close 后，对端回的 Close 必须被处理，关闭握手才算完成。
	 * <p>
	 * 回归点：此前「已发 Close」状态下 decodeFrame 直接丢弃所有帧，对端回的 Close 也被吞掉，
	 * 关闭握手永远走不到完成分支，只能等 {@code Ws#DEFAULT_CLOSE_TIMEOUT}（5s）超时强制断开。
	 * </p>
	 */
	@Test
	void serverInitiatedClose_completesHandshakeWhenPeerReplies() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();
			client.sendText("hello");
			assertNotNull(client.readFrame());

			ChannelContext context = contextRef.get();
			assertNotNull(context);
			assertTrue(Ws.close(context, 1000, "bye"));
			WsTestClient.Frame closeFrame = client.readFrame();
			assertNotNull(closeFrame);
			assertEquals(0x8, closeFrame.opcode);

			// 对端回一个 Close，关闭握手应当在毫秒级完成，而不是等 5s 超时
			client.sendClose(1000, "bye");
			client.setReadTimeout(2500);
			long start = System.currentTimeMillis();
			boolean closed = client.isClosedByServer();
			long elapsed = System.currentTimeMillis() - start;

			assertTrue(closed, "对端回 Close 后服务端应关闭 TCP 连接");
			assertTrue(elapsed < 2500, "关闭握手应在超时前完成，实际耗时：" + elapsed + "ms");
			WsSessionContext session = context.get(WsSessionContext.WS_SESSION_CONTEXT_KEY);
			assertNotNull(session);
			assertTrue(session.isCloseReceived(), "服务端应记录到对端的 Close");
		}
	}

	/**
	 * 关闭握手期间客户端又发来数据帧时，关闭握手仍要立即完成。
	 * <p>
	 * 回归点：已发 Close 状态下丢弃数据帧时，decode 返回 null 会让框架把读指针回退到帧头，
	 * 这个数据帧被还回去、反复解析（还会累加解码失败次数），排在它后面的对端 Close 帧
	 * 就永远解析不到，关闭握手只能等 5s 超时。
	 * </p>
	 */
	@Test
	void serverInitiatedClose_completesHandshakeWhenDataFrameArrivesFirst() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();
			client.sendText("hello");
			assertNotNull(client.readFrame());

			ChannelContext context = contextRef.get();
			assertNotNull(context);
			assertTrue(Ws.close(context, 1000, "bye"));
			assertNotNull(client.readFrame());

			// 关闭握手期间客户端又发了一条数据帧，紧接着才是 Close
			client.sendText("late");
			client.sendClose(1000, "bye");

			client.setReadTimeout(2500);
			long start = System.currentTimeMillis();
			boolean closed = client.isClosedByServer();
			long elapsed = System.currentTimeMillis() - start;

			assertTrue(closed, "对端回 Close 后服务端应关闭 TCP 连接");
			assertTrue(elapsed < 2500, "关闭握手应在超时前完成，实际耗时：" + elapsed + "ms");
			WsSessionContext session = context.get(WsSessionContext.WS_SESSION_CONTEXT_KEY);
			assertNotNull(session);
			assertTrue(session.isCloseReceived(), "服务端应记录到对端的 Close");
		}
	}

	/**
	 * 协议错误的坏帧必须被消费掉，不能留在缓冲区里反复解析。
	 * <p>
	 * 回归点：协议错误分支 decode 返回 null，框架会把读指针回退到坏帧的帧头，
	 * 每次读事件都重新解析同一个坏帧、反复抛异常，解码失败次数不断累加
	 * （累加到阈值会被当成慢包攻击粗暴断连），坏帧之后的数据也永远解析不到。
	 * </p>
	 */
	@Test
	void protocolError_doesNotLeaveBadFrameInBuffer() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();
			// 先发一条业务消息，让服务端记录下连接上下文
			client.sendText("hello");
			assertNotNull(client.readFrame());

			// 0x3 是协议保留的操作码
			client.sendFrame(0x3, new byte[]{'a'}, true, true);
			assertNotNull(client.readFrame());

			ChannelContext context = contextRef.get();
			assertNotNull(context);
			assertEquals(0, context.stat.decodeFailCount,
				"协议错误后坏帧应被消费掉，不应累加解码失败次数");
		}
	}

	@Test
	void unknownOpcode_triggersCloseWithProtocolError() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();
			// 0x3 是协议保留的操作码
			client.sendFrame(0x3, new byte[]{'a'}, true, true);

			WsTestClient.Frame frame = client.readFrame();

			assertNotNull(frame);
			assertEquals(0x8, frame.opcode);
			// 1002 协议错误
			assertEquals(0x03, frame.payload[0] & 0xFF);
			assertEquals(0xEA, frame.payload[1] & 0xFF);
		}
	}

	@Test
	void unmaskedFrame_triggersCloseWithProtocolError() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();
			// 客户端发往服务端的帧必须带掩码
			client.sendFrame(0x1, "hello".getBytes(StandardCharsets.UTF_8), true, false);

			WsTestClient.Frame frame = client.readFrame();

			assertNotNull(frame);
			assertEquals(0x8, frame.opcode);
			assertEquals(0x03, frame.payload[0] & 0xFF);
			assertEquals(0xEA, frame.payload[1] & 0xFF);
		}
	}

	@Test
	void invalidTextUtf8_triggersCloseWithInvalidPayload() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();
			// 非法的 UTF-8 序列
			client.sendFrame(0x1, new byte[]{(byte) 0xC3, (byte) 0x28}, true, true);

			WsTestClient.Frame frame = client.readFrame();

			assertNotNull(frame);
			assertEquals(0x8, frame.opcode);
			// 1007 无效的负载数据
			assertEquals(0x03, frame.payload[0] & 0xFF);
			assertEquals(0xEF, frame.payload[1] & 0xFF);
		}
	}

	/**
	 * RFC 6455 §5.4：续帧必须跟在分片消息之后，前面没有首帧属于协议错误。
	 */
	@Test
	void continuationFrameWithoutStart_triggersCloseWithProtocolError() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();
			// 直接发续帧，前面没有分片消息
			client.sendFrame(0x0, "orphan".getBytes(StandardCharsets.UTF_8), true, true);

			WsTestClient.Frame frame = client.readFrame();

			assertNotNull(frame);
			assertEquals(0x8, frame.opcode);
			// 1002 协议错误
			assertEquals(0x03, frame.payload[0] & 0xFF);
			assertEquals(0xEA, frame.payload[1] & 0xFF);
		}
	}

	/**
	 * RFC 6455 §5.4：分片消息进行中只允许接收续帧，插入新的数据帧属于协议错误。
	 */
	@Test
	void newDataFrameDuringFragmentation_triggersCloseWithProtocolError() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();
			client.sendFrame(0x1, "part1".getBytes(StandardCharsets.UTF_8), false, true);
			// 分片还没结束就插入新的数据帧
			client.sendFrame(0x1, "part2".getBytes(StandardCharsets.UTF_8), true, true);

			WsTestClient.Frame frame = client.readFrame();

			assertNotNull(frame);
			assertEquals(0x8, frame.opcode);
			// 1002 协议错误
			assertEquals(0x03, frame.payload[0] & 0xFF);
			assertEquals(0xEA, frame.payload[1] & 0xFF);
		}
	}

	/**
	 * RFC 6455 §7.4.1：1005 是保留码，不得出现在 Close 帧中，收到时按协议错误处理。
	 */
	@Test
	void reservedCloseCode_triggersCloseWithProtocolError() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();
			client.sendClose(1005, "bye");

			WsTestClient.Frame frame = client.readFrame();

			assertNotNull(frame);
			assertEquals(0x8, frame.opcode);
			// 1002 协议错误
			assertEquals(0x03, frame.payload[0] & 0xFF);
			assertEquals(0xEA, frame.payload[1] & 0xFF);
		}
	}

	/**
	 * RFC 6455 §4.2.2：服务端从自己支持的子协议里选一个回给客户端，优先级以服务端为准。
	 */
	@Test
	void handshake_withSubProtocol_selectsSupportedOne() throws Exception {
		int subPort = findFreePort();
		WsServerStarter subStarter = new WsServerStarter(subPort, new IWsMsgHandler() {
			@Override
			public Object onText(WsRequest request, String text, ChannelContext context) {
				return text;
			}

			@Override
			public Object onBytes(WsRequest request, byte[] bytes, ChannelContext context) {
				return bytes;
			}

			@Override
			public String[] getSupportedSubProtocols() {
				return new String[]{"chat", "superchat"};
			}
		});
		subStarter.start();
		try (WsTestClient client = new WsTestClient(subPort)) {
			client.handshake("GET /ws HTTP/1.1", "Upgrade: websocket\r\nConnection: Upgrade\r\n"
				+ "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\nSec-WebSocket-Version: 13\r\n"
				+ "Sec-WebSocket-Protocol: superchat, chat\r\n");

			assertEquals(101, client.getStatusCode());
			assertEquals("chat", client.getHeader("Sec-WebSocket-Protocol"),
				"服务端应按自己的优先级回选子协议");
		} finally {
			subStarter.stop();
		}
	}

	/**
	 * 客户端请求的子协议服务端都不支持时，握手响应里不带上子协议头。
	 */
	@Test
	void handshake_withUnsupportedSubProtocol_returnsNoProtocolHeader() throws Exception {
		int subPort = findFreePort();
		WsServerStarter subStarter = new WsServerStarter(subPort, new IWsMsgHandler() {
			@Override
			public Object onText(WsRequest request, String text, ChannelContext context) {
				return text;
			}

			@Override
			public Object onBytes(WsRequest request, byte[] bytes, ChannelContext context) {
				return bytes;
			}

			@Override
			public String[] getSupportedSubProtocols() {
				return new String[]{"chat"};
			}
		});
		subStarter.start();
		try (WsTestClient client = new WsTestClient(subPort)) {
			client.handshake("GET /ws HTTP/1.1", "Upgrade: websocket\r\nConnection: Upgrade\r\n"
				+ "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\nSec-WebSocket-Version: 13\r\n"
				+ "Sec-WebSocket-Protocol: unknown\r\n");

			assertEquals(101, client.getStatusCode());
			assertNull(client.getHeader("Sec-WebSocket-Protocol"));
		} finally {
			subStarter.stop();
		}
	}

	/**
	 * RFC 6455 §5.5.2：关闭握手中收到 Ping 仍要回 Pong（除非已经收到对端的 Close）。
	 */
	@Test
	void pingAfterServerClose_isAnsweredWithPong() throws Exception {
		try (WsTestClient client = new WsTestClient(port)) {
			client.handshake();
			client.sendText("hello");
			assertNotNull(client.readFrame());

			ChannelContext context = contextRef.get();
			assertNotNull(context);
			assertTrue(Ws.close(context, 1000, "bye"));
			// 读掉服务端发来的 Close
			WsTestClient.Frame closeFrame = client.readFrame();
			assertNotNull(closeFrame);
			assertEquals(0x8, closeFrame.opcode);

			byte[] data = "late-ping".getBytes(StandardCharsets.UTF_8);
			client.sendPing(data);
			WsTestClient.Frame pongFrame = client.readFrame();

			assertNotNull(pongFrame);
			assertEquals(0xA, pongFrame.opcode);
			assertArrayEquals(data, pongFrame.payload);
		}
	}

	/**
	 * 等待服务端关闭连接
	 *
	 * @param client 客户端
	 * @return true 表示服务端已经关闭连接
	 * @throws IOException IOException
	 */
	private static boolean waitForServerClose(WsTestClient client) throws IOException {
		long deadline = System.currentTimeMillis() + 3000;
		while (System.currentTimeMillis() < deadline) {
			if (client.isClosedByServer()) {
				return true;
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return false;
	}

	/**
	 * 找一个空闲端口
	 *
	 * @return 端口号
	 * @throws IOException IOException
	 */
	private static int findFreePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}
}
