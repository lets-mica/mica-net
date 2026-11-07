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
