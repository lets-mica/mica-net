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
import net.dreamlu.mica.net.client.ReconnConf;
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
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 端到端场景测试：普通 TCP / TLS，以及断线重连后的数据收发。
 *
 * @author L.cm
 */
class NetE2EReconnectTest {
	private static final String KEY_STORE = "classpath:test.jks";
	private static final String KEY_STORE_PWD = "501937";
	private static final long TIMEOUT_MS = 20_000L;

	@Test
	void plainTcpShouldExchangeData() throws Exception {
		runScenario(false, false);
	}

	@Test
	void sslShouldExchangeData() throws Exception {
		runScenario(true, false);
	}

	@Test
	void plainTcpShouldReconnectAndExchangeData() throws Exception {
		runScenario(false, true);
	}

	@Test
	void sslShouldReconnectAndExchangeData() throws Exception {
		runScenario(true, true);
	}

	private void runScenario(boolean ssl, boolean reconnect) throws Exception {
		// 随机可用端口，避免与本机其它服务冲突
		int port;
		try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
			port = socket.getLocalPort();
		}
		int expect = reconnect ? 2 : 1;
		CountDownLatch serverReceived = new CountDownLatch(expect);
		// 重连场景下服务端只在第二次交互时回包，客户端只会收到一次 pong
		CountDownLatch clientReceived = new CountDownLatch(1);
		AtomicReference<String> serverPayload = new AtomicReference<>();
		AtomicReference<String> clientPayload = new AtomicReference<>();
		AtomicInteger serverRecvCount = new AtomicInteger();

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
				if (reconnect && serverRecvCount.incrementAndGet() == 1) {
					// 第一次交互后主动断开，触发客户端重连
					Tio.close(ctx, "test close for reconnect");
					return;
				}
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

		TioServerConfig serverConfig = new TioServerConfig("e2e-server", serverHandler, null);
		if (ssl) {
			serverConfig.setSslConfig(SslConfig.forServer(KEY_STORE, KEY_STORE_PWD));
		}
		TioServer server = new TioServer(port, serverConfig);
		server.start();

		TioClientConfig clientConfig = new TioClientConfig(clientHandler, null);
		if (ssl) {
			clientConfig.setSslConfig(SslConfig.forClient(KEY_STORE, KEY_STORE_PWD));
		}
		if (reconnect) {
			clientConfig.setReconnConf(new ReconnConf(200L, -1));
		}
		TioClient client = new TioClient(clientConfig);

		AtomicReference<ClientChannelContext> ctxRef = new AtomicReference<>();
		Thread sender = new Thread(() -> {
			while (serverReceived.getCount() > 0) {
				try {
					ClientChannelContext ctx = ctxRef.get();
					if (ctx != null) {
						Tio.send(ctx, new EncodedPacket("ping\n".getBytes(StandardCharsets.UTF_8)));
					}
				} catch (Throwable e) {
					// 断线重连期间发送失败是正常的
				}
				try {
					Thread.sleep(200L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		});
		sender.setDaemon(true);

		try {
			ClientChannelContext context = client.connect(new Node("127.0.0.1", port), 5000);
			assertNotNull(context, "客户端应连接成功");
			ctxRef.set(context);

			sender.start();

			assertTrue(serverReceived.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "服务端未收到数据");
			assertEquals("ping", serverPayload.get());

			assertTrue(clientReceived.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "客户端未收到响应");
			assertEquals("pong", clientPayload.get());
		} finally {
			client.stop();
			server.stop();
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
