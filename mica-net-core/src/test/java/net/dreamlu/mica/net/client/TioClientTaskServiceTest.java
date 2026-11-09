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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 客户端定时任务服务的归属测试。
 * <p>
 * 背景：{@link TioClient} 构造时会自己兜底创建一个 {@link TimerTaskService}，业务侧只能通过
 * {@link TioConfig} 拿定时器的组件（如 websocket 关闭握手超时兜底）在客户端侧永远拿到 null，
 * 退化成「立即断开 TCP」，对端回应的 Close 帧根本来不及处理。
 * </p>
 * <p>
 * 这里同时锁住两条归属规则：
 * ① 框架自建的时间轮登记在 {@code getRuntimeTaskService()}，不占用「用户设置的」那个字段；
 * ② 用户显式传入的时间轮与框架自建的时间轮，客户端都不负责停。
 * </p>
 *
 * @author L.cm
 */
class TioClientTaskServiceTest {

	/**
	 * 框架自建的时间轮要能被业务侧取到（websocket 关闭握手超时兜底靠它），
	 * 但不能写进「用户设置的」那个字段，否则同一份配置再建一个客户端时
	 * 会把这份实例当成用户提供的直接复用。
	 */
	@Test
	void clientConfigShouldExposeRuntimeTaskService() throws Exception {
		TioClientConfig config = new TioClientConfig(new NoopHandler(), new DefaultTioClientListener());
		// 关掉默认心跳，避免定时任务干扰
		config.setHeartbeatTimeout(0);
		TioClient client = new TioClient(config);
		try {
			TimerTaskService runtime = config.getRuntimeTaskService();
			assertNotNull(runtime, "TioClient 应把实际使用的定时任务服务登记到运行时的引用上");
			assertSame(client.getTaskService(), runtime, "运行时的定时器应与客户端实际使用的一致");
			assertNull(config.getTaskService(), "框架自建的时间轮不应占用「用户设置的」字段");
		} finally {
			client.stop();
		}
	}

	/**
	 * 同一份配置建两个客户端时，两个客户端必须各自拥有时间轮。
	 * <p>
	 * 否则停掉其中一个会把另一个的心跳与重连任务一起停掉——共享时间轮的那份实例
	 * 是第一个客户端自建的，第二个客户端只是被动继承。
	 * </p>
	 */
	@Test
	void twoClientsWithSameConfigShouldOwnSeparateTaskService() throws Exception {
		TioClientConfig config = new TioClientConfig(new NoopHandler(), new DefaultTioClientListener());
		config.setHeartbeatTimeout(0);

		TioClient client1 = new TioClient(config);
		TioClient client2 = new TioClient(config);
		try {
			assertNotSame(client1.getTaskService(), client2.getTaskService(),
				"同一份配置下的两个客户端应各自拥有时间轮，不能共享");

			// 停掉 client1，client2 的定时器必须仍然可用
			client1.stop();
			assertTrue(runTaskAndWait(client2.getTaskService()), "client1 stop 之后 client2 的定时器仍应能执行任务");
		} finally {
			client2.stop();
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
		assertTrue(runTaskAndWait(external), "客户端 stop 之后外部定时器仍应能执行任务");
		external.stop();
	}

	/**
	 * 在指定定时器上注册一个延时任务并等待它执行
	 *
	 * @param taskService 定时任务服务
	 * @return true 表示任务在等待期内执行了
	 */
	private static boolean runTaskAndWait(TimerTaskService taskService) throws InterruptedException {
		CountDownLatch ran = new CountDownLatch(1);
		taskService.addTask(systemTimer -> new TimerTask(10) {
			@Override
			public void run() {
				ran.countDown();
			}
		});
		taskService.start();
		return ran.await(5, TimeUnit.SECONDS);
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
