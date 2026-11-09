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

package net.dreamlu.mica.net.client;

import net.dreamlu.mica.net.client.intf.TioClientHandler;
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.TioConfig;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.utils.timer.DefaultTimerTaskService;
import net.dreamlu.mica.net.utils.timer.TimerTask;
import net.dreamlu.mica.net.utils.timer.TimerTaskService;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 客户端定时任务服务的可见性测试。
 * <p>
 * 背景：{@link TioClient} 构造时会自己兜底创建一个 {@link TimerTaskService}，但不回写配置，
 * 于是只能通过 {@link TioConfig} 拿定时器的组件（如 websocket 关闭握手超时兜底）在客户端侧
 * 永远拿到 null，退化成「立即断开 TCP」，对端回应的 Close 帧根本来不及处理。
 * </p>
 *
 * @author L.cm
 */
class TioClientTaskServiceTest {

	@Test
	void clientConfigShouldExposeTaskService() throws Exception {
		TioClientConfig config = new TioClientConfig(new NoopHandler(), new DefaultTioClientListener());
		// 关掉默认心跳，避免定时任务干扰
		config.setHeartbeatTimeout(0);
		TioClient client = new TioClient(config);
		try {
			TimerTaskService fromConfig = config.getTaskService();
			assertNotNull(fromConfig, "TioClient 应把实际使用的定时任务服务回写进配置");
			assertSame(client.getTaskService(), fromConfig, "配置里的定时器应与客户端实际使用的一致");
		} finally {
			client.stop();
		}
	}

	/**
	 * 用户显式传入的时间轮可能被多个客户端共享，停掉一个客户端不能把它关掉。
	 * <p>
	 * {@code DefaultTimerTaskService#stop()} 不可逆（时间轮线程 shutdown 后无法再 start），
	 * 误停会让其他客户端的心跳与重连任务全部停摆。
	 * </p>
	 */
	@Test
	void stopShouldNotStopExternalTaskService() throws Exception {
		TioClientConfig config = new TioClientConfig(new NoopHandler(), new DefaultTioClientListener());
		config.setHeartbeatTimeout(0);
		TimerTaskService external = new DefaultTimerTaskService();
		config.setTaskService(external);
		TioClient client = new TioClient(config);
		assertSame(external, client.getTaskService(), "客户端应直接使用外部传入的定时器");
		client.stop();

		// 外部定时器必须仍然可用：还能继续注册并执行任务
		CountDownLatch ran = new CountDownLatch(1);
		external.addTask(systemTimer -> new TimerTask(10) {
			@Override
			public void run() {
				ran.countDown();
			}
		});
		external.start();
		assertTrue(ran.await(5, TimeUnit.SECONDS), "客户端 stop 之后外部定时器仍应能执行任务");
		external.stop();
	}

	/**
	 * 只用来占位的处理器，本用例不建立连接
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
