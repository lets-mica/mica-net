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

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.TioConfig;
import net.dreamlu.mica.net.core.task.AbstractSendRunnable;
import net.dreamlu.mica.net.utils.timer.TimerTask;
import net.dreamlu.mica.net.utils.timer.TimerTaskService;

/**
 * TLS 优雅关闭：在关闭 socket 之前把 TLS close_notify 记录发出去，让对端感知到是本端主动断开。
 * <p>
 * 为什么不能走发送队列：连接关闭流程里，发送任务已经被取消、socket 也即将关闭，
 * 此时把 close_notify 塞进发送队列只会得到两个结果——被 {@code addMsg} 的取消判断直接丢弃，
 * 或者被 {@code checkBeforeIO} 的 waitingClose 判断拦下，两条路都发不出去。
 * 所以这里绕过发送队列，直接把记录写进 socket。
 * </p>
 * <p>
 * 为什么是异步：{@code Tio.close} 的调用方基本都是 IO 线程或 handler 线程，若在这里等一次
 * socket 写完成，关闭动作就变成了「调用方线程阻塞等内核」，连接多时还会互相拖慢。
 * 因此这里只负责<b>发起</b>写，写完成（或超时、失败）后由回调执行后续关闭动作
 * {@code afterClose}——socket 的关闭被推迟到 close_notify 之后，顺序不变但调用方不再阻塞。
 * </p>
 * <p>
 * 抢不到写状态时怎么办：连接关闭时往往还有框架的在途写（业务高峰期常态）。
 * 此前这里是「抢不到就直接放弃 close_notify」，等于把「close_notify 发不出去」这个
 * 老问题又带了回来。现在改成挂一个「在途写结束后重试」的钩子：在途写一结束就自动补发，
 * 既不阻塞调用方线程，也不轻易放弃。兜底用定时器：在途写迟迟不结束时放弃 close_notify
 * 并关闭连接，避免连接因为等待一个永不结束的写而悬着。
 * </p>
 *
 * @author L.cm
 */
public final class SslGracefulShutdown {
	private static final Logger log = LoggerFactory.getLogger(SslGracefulShutdown.class);
	/**
	 * close_notify 写出的最长等待时间（毫秒）。
	 * <p>
	 * 超时后 JDK 会让本次写以 {@code InterruptedByTimeoutException} 失败并关闭 channel，
	 * 关闭动作照常在失败回调里执行，不会因为对端不读数据而卡住关闭流程。
	 * </p>
	 */
	private static final long CLOSE_NOTIFY_WRITE_TIMEOUT = 100L;
	/**
	 * 等待在途写结束的最长时间（毫秒），超过就放弃 close_notify 并关闭连接。
	 */
	private static final long WAIT_IN_FLIGHT_WRITE_TIMEOUT = 50L;
	/**
	 * 等待在途写结束时的最大重试次数。
	 */
	private static final int MAX_RETRY = 3;

	/**
	 * 关闭流程状态：还没把 close_notify 发出去
	 */
	private static final int STATE_INIT = 0;
	/**
	 * 关闭流程状态：close_notify 已经发起写
	 */
	private static final int STATE_SENT = 1;
	/**
	 * 关闭流程状态：已经收尾（afterClose 已执行或已放弃 close_notify）
	 */
	private static final int STATE_DONE = 2;

	private SslGracefulShutdown() {
	}

	/**
	 * 优雅关闭：尽力发出 close_notify，然后执行后续关闭动作。
	 * <p>
	 * 以下情况会直接执行 {@code afterClose}，不再尝试发送 close_notify：
	 * 非 SSL 连接、握手未完成（对端还没建立会话，发通知意义不大）、通道已关闭、
	 * 产出记录失败。
	 * </p>
	 *
	 * @param channelContext 连接上下文
	 * @param afterClose     后续关闭动作（关闭 socket、投递关闭任务）。
	 *                       可能在当前线程同步执行，也可能在写完成回调里执行，实现需幂等
	 */
	public static void closeGracefully(ChannelContext channelContext, Runnable afterClose) {
		// afterClose 只允许执行一次：重试、兜底、写完成回调都可能走到收尾
		AtomicBoolean once = new AtomicBoolean(false);
		closeGracefully(channelContext, afterClose, once, new AtomicInteger(0), new AtomicInteger(STATE_INIT));
	}

	/**
	 * 优雅关闭（内部实现，支持重试）
	 *
	 * @param channelContext 连接上下文
	 * @param afterClose     后续关闭动作
	 * @param once           保证 afterClose 只执行一次
	 * @param attempts       已重试次数
	 * @param state          关闭流程状态
	 */
	private static void closeGracefully(ChannelContext channelContext, Runnable afterClose,
										AtomicBoolean once, AtomicInteger attempts, AtomicInteger state) {
		SslHandler sslHandler = channelContext.getSslHandler();
		AsynchronousSocketChannel socket = channelContext.asynchronousSocketChannel;
		if (sslHandler == null || !sslHandler.isHandshakeCompleted() || socket == null || !socket.isOpen()) {
			finish(channelContext, once, afterClose);
			return;
		}
		// 抢占写状态：单纯「先查 isWriting 再 write」中间有窗口，
		// 框架写线程可能恰好在此刻发起写，两边都调 write 就会撞 WritePendingException。
		AbstractSendRunnable sendRunnable = channelContext.getSendRunnable();
		if (sendRunnable != null && !sendRunnable.tryStartWriting()) {
			// 有在途写：挂一个「在途写结束后重试」的钩子，不阻塞调用方线程
			if (attempts.get() < MAX_RETRY && sendRunnable.registerAfterWrite(
					() -> closeGracefully(channelContext, afterClose, once, attempts, state))) {
				// 兜底：在途写迟迟不结束时放弃 close_notify 并关闭连接，避免连接悬着
				scheduleFallback(channelContext, afterClose, once, state);
				return;
			}
			log.debug("{}, 有在途写操作，放弃发送 TLS close_notify", channelContext);
			finish(channelContext, once, afterClose);
			return;
		}
		ByteBuffer closeNotify;
		try {
			// 让引擎产出 close_notify 记录（纯内存操作，不涉及 IO）
			closeNotify = sslHandler.closeGracefully();
		} catch (Throwable e) {
			log.warn("{}, 产出 TLS close_notify 失败:{}", channelContext, e.getMessage());
			releaseWriting(sendRunnable);
			finish(channelContext, once, afterClose);
			return;
		}
		if (closeNotify == null) {
			releaseWriting(sendRunnable);
			finish(channelContext, once, afterClose);
			return;
		}
		state.set(STATE_SENT);
		try {
			socket.write(new ByteBuffer[]{closeNotify}, 0, 1, CLOSE_NOTIFY_WRITE_TIMEOUT, TimeUnit.MILLISECONDS, null,
				new CloseNotifyHandler(channelContext, sendRunnable, closeNotify, once, afterClose, state));
		} catch (Throwable e) {
			log.warn("{}, 写入 TLS close_notify 失败:{}", channelContext, e.getMessage());
			releaseWriting(sendRunnable);
			finish(channelContext, once, afterClose);
		}
	}

	/**
	 * 兜底：等待在途写结束超过预算就放弃 close_notify 并关闭连接。
	 * <p>
	 * 在途写理论上可能一直不结束（写没有超时），那时若还傻等，连接就会一直悬在
	 * 「已请求关闭但 socket 没关」的状态。有了这个兜底，最多延迟
	 * {@link #WAIT_IN_FLIGHT_WRITE_TIMEOUT} 就会收尾。
	 * </p>
	 *
	 * @param channelContext 连接上下文
	 * @param afterClose     后续关闭动作
	 * @param once           保证 afterClose 只执行一次
	 * @param state          关闭流程状态
	 */
	private static void scheduleFallback(ChannelContext channelContext, Runnable afterClose,
										 AtomicBoolean once, AtomicInteger state) {
		TimerTaskService taskService = channelContext.tioConfig.getRuntimeTaskService();
		if (taskService == null) {
			// 没有定时器就没法兜底：直接放弃 close_notify 并关闭连接，
			// 不能为了 close_notify 让连接一直悬着
			log.debug("{}, 没有可用的定时任务服务，放弃发送 TLS close_notify", channelContext);
			if (state.compareAndSet(STATE_INIT, STATE_DONE)) {
				finish(channelContext, once, afterClose);
			}
			return;
		}
		taskService.addTask(systemTimer -> new TimerTask(WAIT_IN_FLIGHT_WRITE_TIMEOUT) {
			@Override
			public void run() {
				// 状态仍是 INIT 说明 close_notify 还没发出去（重试也没成功），此时收尾
				if (state.compareAndSet(STATE_INIT, STATE_DONE)) {
					log.debug("{}, 等待在途写结束超时，放弃发送 TLS close_notify", channelContext);
					finish(channelContext, once, afterClose);
				}
			}
		});
	}

	/**
	 * 释放写状态
	 *
	 * @param sendRunnable 发送任务，可为 null
	 */
	private static void releaseWriting(AbstractSendRunnable sendRunnable) {
		if (sendRunnable != null) {
			sendRunnable.finishWriting();
		}
	}

	/**
	 * 执行后续关闭动作，只执行一次，异常不外抛：关闭流程已经走到这一步，
	 * 不能因为回调异常让连接卡在 waitingClose 状态
	 *
	 * @param channelContext 连接上下文
	 * @param once           保证只执行一次
	 * @param afterClose     后续关闭动作
	 */
	private static void finish(ChannelContext channelContext, AtomicBoolean once, Runnable afterClose) {
		if (!once.compareAndSet(false, true)) {
			return;
		}
		try {
			afterClose.run();
		} catch (Throwable e) {
			log.error("{}, 连接关闭动作执行失败:{}", channelContext, e.getMessage(), e);
		}
	}

	/**
	 * 记录 close_notify 的发送统计。
	 * <p>
	 * 这条记录是直写 socket 的，绕过了 {@code WriteCompletionHandler}，统计需要在这里补上，
	 * 否则 {@code sentBytes} 会比实际发出的字节数少一段。业务包监听（{@code PacketListener}）
	 * 不适用于 close_notify——它是 TLS 协议记录，不是业务包。
	 * </p>
	 *
	 * @param channelContext 连接上下文
	 * @param bytesWritten   写入字节数
	 */
	private static void recordSentBytes(ChannelContext channelContext, long bytesWritten) {
		channelContext.stat.latestTimeOfSentPacket = System.currentTimeMillis();
		TioConfig tioConfig = channelContext.tioConfig;
		if (tioConfig.statOn) {
			tioConfig.groupStat.sentBytes.add(bytesWritten);
			channelContext.stat.sentBytes.addAndGet(bytesWritten);
		}
	}

	/**
	 * close_notify 写完成回调：补统计、释放写状态、执行后续关闭动作
	 */
	private static final class CloseNotifyHandler implements CompletionHandler<Long, Object> {
		private final ChannelContext channelContext;
		private final AbstractSendRunnable sendRunnable;
		private final Runnable afterClose;
		/**
		 * 保证 afterClose 只执行一次
		 */
		private final AtomicBoolean once;
		/**
		 * 关闭流程状态
		 */
		private final AtomicInteger state;
		/**
		 * 待写出的 close_notify 记录，写完成后缓冲区 position 会前移，用于判断是否还有剩余
		 */
		private final ByteBuffer closeNotify;

		private CloseNotifyHandler(ChannelContext channelContext, AbstractSendRunnable sendRunnable,
								   ByteBuffer closeNotify, AtomicBoolean once, Runnable afterClose, AtomicInteger state) {
			this.channelContext = channelContext;
			this.sendRunnable = sendRunnable;
			this.closeNotify = closeNotify;
			this.once = once;
			this.afterClose = afterClose;
			this.state = state;
		}

		@Override
		public void completed(Long result, Object attachment) {
			long bytesWritten = result == null ? 0L : result;
			if (bytesWritten <= 0) {
				log.warn("{}, 写入 TLS close_notify 返回:{}", channelContext, bytesWritten);
				done();
				return;
			}
			recordSentBytes(channelContext, bytesWritten);
			if (closeNotify.hasRemaining()) {
				// 极少数情况下记录只写出一部分，继续写剩余部分，避免对端收到半个 TLS 记录
				if (!writeRemaining()) {
					done();
				}
				return;
			}
			done();
		}

		@Override
		public void failed(Throwable exc, Object attachment) {
			log.warn("{}, 写入 TLS close_notify 失败:{}", channelContext,
				exc == null ? "未知异常" : exc.getMessage());
			done();
		}

		/**
		 * 继续写剩余部分
		 *
		 * @return true 表示已发起续写，关闭动作由回调继续；false 表示无法续写
		 */
		private boolean writeRemaining() {
			AsynchronousSocketChannel socket = channelContext.asynchronousSocketChannel;
			if (socket == null || !socket.isOpen() || !closeNotify.hasRemaining()) {
				return false;
			}
			try {
				socket.write(new ByteBuffer[]{closeNotify}, 0, 1, CLOSE_NOTIFY_WRITE_TIMEOUT,
					TimeUnit.MILLISECONDS, null, this);
				return true;
			} catch (Throwable e) {
				log.warn("{}, 续写 TLS close_notify 失败:{}", channelContext, e.getMessage());
				return false;
			}
		}

		private void done() {
			try {
				releaseWriting(sendRunnable);
				// 写状态释放后，队列里可能还堆着包，继续调度一次
				if (sendRunnable != null) {
					sendRunnable.resumeSend();
				}
			} finally {
				if (state.compareAndSet(STATE_SENT, STATE_DONE)) {
					finish(channelContext, once, afterClose);
				}
			}
		}
	}
}
