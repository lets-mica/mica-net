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

package net.dreamlu.mica.net.core.ssl;

import net.dreamlu.mica.net.client.ClientChannelContext;
import net.dreamlu.mica.net.client.TioClient;
import net.dreamlu.mica.net.client.TioClientConfig;
import net.dreamlu.mica.net.client.intf.TioClientHandler;
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.Node;
import net.dreamlu.mica.net.core.Tio;
import net.dreamlu.mica.net.core.TioConfig;
import net.dreamlu.mica.net.core.intf.EncodedPacket;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.server.TioServer;
import net.dreamlu.mica.net.server.TioServerConfig;
import net.dreamlu.mica.net.server.intf.TioServerHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SSL 端到端测试：TioClient 通过 TLS 连接 TioServer 并双向收发数据。
 * <p>
 * 覆盖握手完成、握手后业务包加密发送与解密接收的完整链路。
 * </p>
 *
 * @author L.cm
 */
class SslClientServerIntegrationTest {
	private int port;
	private static final String KEY_STORE = "classpath:test.jks";
	private static final String KEY_STORE_PWD = "501937";
	private static final long TIMEOUT_MS = 10_000L;

	private TioServer server;
	private TioClient client;

	private final CountDownLatch serverReceived = new CountDownLatch(1);
	private final CountDownLatch clientReceived = new CountDownLatch(1);
	private final AtomicReference<String> serverPayload = new AtomicReference<>();
	private final AtomicReference<String> clientPayload = new AtomicReference<>();

	@BeforeEach
	void setUp() throws Exception {
		TioServerHandler serverHandler = new TioServerHandler() {
			@Override
			public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext ctx) {
				return decodeLine(buffer, limit, position);
			}

			@Override
			public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext ctx) {
				return null;
			}

			@Override
			public void handler(Packet packet, ChannelContext ctx) {
				serverPayload.set(new String(((EncodedPacket) packet).getBytes(), StandardCharsets.UTF_8));
				serverReceived.countDown();
				Tio.send(ctx, new EncodedPacket("pong\n".getBytes(StandardCharsets.UTF_8)));
			}
		};

		TioClientHandler clientHandler = new TioClientHandler() {
			@Override
			public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext ctx) {
				return decodeLine(buffer, limit, position);
			}

			@Override
			public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext ctx) {
				return null;
			}

			@Override
			public void handler(Packet packet, ChannelContext ctx) {
				clientPayload.set(new String(((EncodedPacket) packet).getBytes(), StandardCharsets.UTF_8));
				clientReceived.countDown();
			}

			@Override
			public Packet heartbeatPacket(ChannelContext context) {
				return null;
			}
		};

		TioServerConfig serverConfig = new TioServerConfig("ssl-test-server", serverHandler, null);
		serverConfig.setSslConfig(SslConfig.forServer(KEY_STORE, KEY_STORE_PWD));
		port = findFreePort();
		server = new TioServer(port, serverConfig);
		server.start();

		TioClientConfig clientConfig = new TioClientConfig(clientHandler, null);
		clientConfig.setSslConfig(SslConfig.forClient(KEY_STORE, KEY_STORE_PWD));
		client = new TioClient(clientConfig);
	}

	@AfterEach
	void tearDown() {
		if (client != null) {
			client.stop();
		}
		if (server != null) {
			server.stop();
		}
	}

	@Test
	void clientShouldExchangeDataOverSsl() throws Exception {
		ClientChannelContext context = client.connect(new Node("127.0.0.1", port), 5000);
		assertNotNull(context, "客户端应连接成功");

		Tio.send(context, new EncodedPacket("ping\n".getBytes(StandardCharsets.UTF_8)));

		assertTrue(serverReceived.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "服务端未收到客户端数据");
		assertEquals("ping", serverPayload.get());

		assertTrue(clientReceived.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "客户端未收到服务端响应");
		assertEquals("pong", clientPayload.get());
	}

	/**
	 * 取一个空闲端口：写死端口在同机并发构建时会撞车
	 *
	 * @return 空闲端口
	 */
	private static int findFreePort() throws java.io.IOException {
		try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	/**
	 * 以 \n 结尾的一行为一个完整包
	 */
	private static Packet decodeLine(ByteBuffer buffer, int limit, int position) {
		int start = position;
		for (int i = position; i < limit; i++) {
			if (buffer.get(i) == '\n') {
				byte[] body = new byte[i - start];
				for (int j = 0; j < body.length; j++) {
					body[j] = buffer.get(start + j);
				}
				buffer.position(i + 1);
				return new EncodedPacket(body);
			}
		}
		// 半包，不消费缓冲区
		return null;
	}
}
