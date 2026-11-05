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
import net.dreamlu.mica.net.client.intf.TioClientListener;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SSL 握手完成回调里同步发送业务包的回归测试。
 * <p>
 * 背景：2.0.18 为 SSLEngine 加了 engineLock 串行化访问，而握手完成回调
 * （{@code TioListener.onAfterConnected}）此前是在持有 engineLock 时触发的。
 * 客户端如果在该回调里同步阻塞发送（如 mica-mqtt 用 {@code Tio.bSend} 发 CONNECT），
 * 发送线程需要 engineLock 加密，而 engineLock 又被回调线程持有 → 死锁，
 * 表现为“TLS 客户端一直连不上”。
 * </p>
 *
 * @author L.cm
 */
class SslHandshakeCallbackSendTest {
	/** 随机可用端口，避免与本机其它服务冲突 */
	private int port;
	private static final String KEY_STORE = "classpath:test.jks";
	private static final String KEY_STORE_PWD = "501937";
	private static final long TIMEOUT_MS = 5_000L;

	private TioServer server;
	private TioClient client;

	private final CountDownLatch serverReceived = new CountDownLatch(1);
	private final AtomicReference<String> serverPayload = new AtomicReference<>();

	@BeforeEach
	void setUp() throws Exception {
		try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
			port = socket.getLocalPort();
		}
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
			}

			@Override
			public Packet heartbeatPacket(ChannelContext context) {
				return null;
			}
		};

		// 握手完成回调里同步阻塞发送，复现 mica-mqtt 发送 CONNECT 的行为
		TioClientListener clientListener = new TioClientListener() {
			@Override
			public void onAfterConnected(ChannelContext context, boolean isConnected, boolean isReconnect) throws Exception {
				if (!isConnected) {
					return;
				}
				Tio.bSend(context, new EncodedPacket("ping\n".getBytes(StandardCharsets.UTF_8)));
			}
		};

		TioServerConfig serverConfig = new TioServerConfig("ssl-callback-test-server", serverHandler, null);
		serverConfig.setSslConfig(SslConfig.forServer(KEY_STORE, KEY_STORE_PWD));
		server = new TioServer(port, serverConfig);
		server.start();

		TioClientConfig clientConfig = new TioClientConfig(clientHandler, clientListener);
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
	void shouldSendPacketInHandshakeCompletedCallback() throws Exception {
		ClientChannelContext context = client.connect(new Node("127.0.0.1", port), 5000);
		assertTrue(context != null, "客户端应连接成功");

		assertTrue(serverReceived.await(TIMEOUT_MS, TimeUnit.MILLISECONDS),
			"握手完成回调里同步发送的数据，服务端在 " + TIMEOUT_MS + "ms 内没有收到（疑似 engineLock 死锁）");
		assertEquals("ping", serverPayload.get());
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
