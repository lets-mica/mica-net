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
import net.dreamlu.mica.net.server.intf.TioServerListener;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主动关闭方发出 TLS close_notify 的回归测试。
 * <p>
 * 背景：连接关闭流程里，发送任务会先被取消、socket 随后关闭，
 * close_notify 若走发送队列会被丢弃（addMsg 的取消判断）或被 checkBeforeIO 拦下，
 * 结果对端只看到 TCP 断流，收不到 TLS 层的关闭通知。
 * 本用例要求：本地主动关闭后，对端必须以「收到对端 close_notify」的方式感知到断开。
 * </p>
 * <p>
 * 注意关闭时机：必须等一次完整业务往来结束后再关闭。
 * TLS 1.3 服务端在握手后还会补发 NewSessionTicket，若在握手刚完成、票据尚在客户端接收队列里时就关闭 socket，
 * 操作系统会因「接收队列有未读数据」发出 RST，把 close_notify 一并丢掉（对端只能看到 Connection reset），
 * 这属于 TCP 层的固有行为，与 close_notify 是否发出无关，因此用例先做完一次 ping/pong 再关闭。
 * </p>
 *
 * @author L.cm
 */
class SslCloseNotifyTest {
	private static final String KEY_STORE = "classpath:test.jks";
	private static final String KEY_STORE_PWD = "501937";
	private static final long TIMEOUT_MS = 10_000L;

	@Test
	void peerShouldReceiveCloseNotifyWhenLocalCloses() throws Exception {
		int port;
		try (ServerSocket socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		}
		CountDownLatch serverClosed = new CountDownLatch(1);
		AtomicReference<String> serverRemark = new AtomicReference<>();
		CountDownLatch clientHandshakeDone = new CountDownLatch(1);
		CountDownLatch clientGotPong = new CountDownLatch(1);

		TioServerConfig serverConfig = new TioServerConfig("close-notify-server", new TioServerHandler() {
			@Override
			public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext ctx) {
				return decodeLine(buffer, limit, position);
			}

			@Override
			public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext ctx) {
				return packet.getPreEncodedByteBuffer();
			}

			@Override
			public void handler(Packet packet, ChannelContext ctx) {
				Tio.send(ctx, new EncodedPacket("pong\n".getBytes(StandardCharsets.UTF_8)));
			}
		}, new TioServerListener() {
			@Override
			public void onBeforeClose(ChannelContext context, Throwable e, String remark, boolean isNeedRemove) {
				serverRemark.set(remark);
				serverClosed.countDown();
			}
		});
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
				return packet.getPreEncodedByteBuffer();
			}

			@Override
			public void handler(Packet packet, ChannelContext ctx) {
				clientGotPong.countDown();
			}

			@Override
			public Packet heartbeatPacket(ChannelContext context) {
				return null;
			}
		}, new TioClientListener() {
			@Override
			public void onAfterConnected(ChannelContext context, boolean isConnected, boolean isReconnect) {
				// SSL 连接建立时回调，说明握手已完成
				clientHandshakeDone.countDown();
			}
		});
		clientConfig.setSslConfig(SslConfig.forClient(KEY_STORE, KEY_STORE_PWD));
		TioClient client = new TioClient(clientConfig);

		try {
			ClientChannelContext ctx = client.connect(new Node("127.0.0.1", port), 5000);
			assertNotNull(ctx, "客户端应连接成功");
			assertTrue(clientHandshakeDone.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "客户端应完成 TLS 握手");

			// 先完成一次业务往来：回包到达客户端时，接收队列里握手期补发的 TLS 票据必然已被读走，
			// 剩下的关闭动作就不会再触发 RST，close_notify 才能确定性送达
			Tio.send(ctx, new EncodedPacket("ping\n".getBytes(StandardCharsets.UTF_8)));
			assertTrue(clientGotPong.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "客户端未收到服务端响应");

			// 主动关闭
			Tio.close(ctx, null, "测试主动关闭");

			assertTrue(serverClosed.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "服务端应在超时前感知到连接关闭");
			String remark = serverRemark.get();
			assertNotNull(remark, "服务端应记录到关闭原因");
			assertTrue(remark.contains("SSL SessionClosed"),
				"服务端应通过 TLS close_notify 感知到主动断开，实际关闭原因: " + remark);
		} finally {
			client.stop();
			server.stop();
		}
	}

	/**
	 * 以 \n 结尾的一行为一个完整包
	 *
	 * @param buffer   buffer
	 * @param limit    limit
	 * @param position position
	 * @return 包，数据不完整时返回 null
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
