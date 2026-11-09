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

package net.dreamlu.mica.net.core.tcp;

import net.dreamlu.mica.net.client.ClientChannelContext;
import net.dreamlu.mica.net.client.DefaultTioClientListener;
import net.dreamlu.mica.net.client.TioClientConfig;
import net.dreamlu.mica.net.client.intf.TioClientHandler;
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.TioConfig;
import net.dreamlu.mica.net.core.intf.Packet;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TCP 写状态互斥测试。
 * <p>
 * 背景：{@code writing} 状态位此前只在发起 {@code write()} 时无条件 {@code set(true)}，
 * 「检查空闲」与「置位」之间隔着编码与 SSL 加密，关闭流程在这个窗口里 CAS 抢占成功后，
 * 框架仍会发起第二次写并撞上 {@code WritePendingException}（包丢失、状态位被误清）。
 * 现在所有发起写的地方都用 CAS 抢占，抢不到就把包放回队列。
 * </p>
 *
 * @author L.cm
 */
class TcpSendRunnableWritingTest {

	/**
	 * 有写在途时不应再提交任务，否则 runTask 抢不到写状态会直接返回，
	 * AbstractSynRunnable 每次都在末尾重新提交，形成空转。
	 */
	@Test
	void isNeededExecute_shouldBeFalseWhileWriting() {
		TcpSendRunnable sendRunnable = newSendRunnable();
		sendRunnable.getMsgQueue().add(newPacket());

		assertTrue(sendRunnable.isNeededExecute(), "队列有包时应需要调度");
		assertTrue(sendRunnable.tryStartWriting(), "空闲时应能抢占写状态");
		assertFalse(sendRunnable.isNeededExecute(), "有写在途时不应再提交任务");
		assertFalse(sendRunnable.tryStartWriting(), "写状态被占用时再次抢占应失败");

		// 写完成回调释放状态位，队列还有包，应重新需要调度
		sendRunnable.onWriteCompleted();
		assertTrue(sendRunnable.isNeededExecute(), "写完成后队列非空应重新调度");
	}

	/**
	 * 直发路径（{@code useQueueSend=false}、SSL 握手记录）在写状态被占用时不能发起写，
	 * 必须把包放回队列，而不是撞 WritePendingException 后丢掉。
	 */
	@Test
	void sendPacket_shouldRequeueWhenWriteInProgress() throws Exception {
		try (ServerSocket serverSocket = new ServerSocket(0)) {
			AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
			try {
				channel.connect(new InetSocketAddress("127.0.0.1", serverSocket.getLocalPort()))
					.get(5, TimeUnit.SECONDS);
				ChannelContext context = newContext(channel);
				// ChannelContext 初始化时默认 closed=true，真实连接建立后才由 ConnectionCompletionHandler 置回 false
				context.setClosed(false);
				TcpSendRunnable sendRunnable = new TcpSendRunnable(context, noopExecutor());

				// 模拟关闭流程已抢占写状态（正在直写 close_notify）
				assertTrue(sendRunnable.tryStartWriting());

				Packet packet = newPacket();
				assertTrue(sendRunnable.sendPacket(packet, false), "包已交给发送任务，应返回 true");

				Queue<Packet> queue = sendRunnable.getMsgQueue();
				assertEquals(1, queue.size(), "抢不到写状态时应把包放回队列");
				assertTrue(queue.contains(packet), "放回队列的应是原包");
			} finally {
				channel.close();
			}
		}
	}

	private static Packet newPacket() {
		Packet packet = new Packet();
		packet.setPreEncodedByteBuffer(ByteBuffer.wrap("ping".getBytes()));
		return packet;
	}

	private static TcpSendRunnable newSendRunnable() {
		ChannelContext context = new ClientChannelContext(newClientConfig());
		return new TcpSendRunnable(context, noopExecutor());
	}

	private static ChannelContext newContext(AsynchronousSocketChannel channel) {
		return new ClientChannelContext(newClientConfig(), channel);
	}

	private static TioClientConfig newClientConfig() {
		return new TioClientConfig(new NoopHandler(), new DefaultTioClientListener());
	}

	private static Executor noopExecutor() {
		return command -> {
		};
	}

	/**
	 * 只用来占位的处理器，本用例直接给出预编码的 ByteBuffer
	 */
	private static final class NoopHandler implements TioClientHandler {
		@Override
		public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext context) {
			return null;
		}

		@Override
		public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext context) {
			return packet.getPreEncodedByteBuffer();
		}

		@Override
		public void handler(Packet packet, ChannelContext context) {
		}

		@Override
		public Packet heartbeatPacket(ChannelContext context) {
			return null;
		}
	}
}
