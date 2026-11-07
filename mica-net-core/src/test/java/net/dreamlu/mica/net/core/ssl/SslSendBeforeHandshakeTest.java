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
 * SSL 握手完成前发送业务包的回归测试。
 * <p>
 * 背景：握手未完成时不能加密业务数据（引擎此时只会产出握手记录，业务数据不会被消费），
 * 此时发送的业务包必须暂存起来，等握手完成后再发出，而不是直接把连接关掉。
 * 这里用 {@code useQueueSend=false} 覆盖"不走发送队列、直接发送"的分支。
 * </p>
 *
 * @author L.cm
 */
class SslSendBeforeHandshakeTest {
	private static final String KEY_STORE = "classpath:test.jks";
	private static final String KEY_STORE_PWD = "501937";
	private static final long TIMEOUT_MS = 10_000L;

	@Test
	void packetSentBeforeHandshakeCompletedShouldStillArrive() throws Exception {
		int port;
		try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
			port = socket.getLocalPort();
		}
		CountDownLatch serverReceived = new CountDownLatch(1);
		AtomicReference<String> serverPayload = new AtomicReference<>();

		TioServerConfig serverConfig = new TioServerConfig("send-before-handshake-server", new TioServerHandler() {
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
		}, null);
		serverConfig.setSslConfig(SslConfig.forServer(KEY_STORE, KEY_STORE_PWD));
		TioServer server = new TioServer(port, serverConfig);
		server.start();

		TioClientConfig clientConfig = new TioClientConfig(new TioClientHandler() {
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
		}, null);
		clientConfig.setSslConfig(SslConfig.forClient(KEY_STORE, KEY_STORE_PWD));
		// 关闭发送队列：发送直接走 sendPacket，不经过 addMsg 的排队逻辑
		clientConfig.setUseQueueSend(false);
		TioClient client = new TioClient(clientConfig);

		try {
			ClientChannelContext ctx = client.connect(new Node("127.0.0.1", port), 5000);
			assertNotNull(ctx, "客户端应连接成功");

			// connect() 返回时握手必然还没完成（握手需要一次网络往返）
			Tio.send(ctx, new EncodedPacket("early\n".getBytes(StandardCharsets.UTF_8)));

			assertTrue(serverReceived.await(TIMEOUT_MS, TimeUnit.MILLISECONDS),
				"握手完成前发送的业务包，服务端在 " + TIMEOUT_MS + "ms 内没有收到（连接很可能被提前关闭）");
			assertEquals("early", serverPayload.get());
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
