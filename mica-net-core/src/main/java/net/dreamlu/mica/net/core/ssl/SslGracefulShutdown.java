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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.TioConfig;
import net.dreamlu.mica.net.core.task.AbstractSendRunnable;

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

	private SslGracefulShutdown() {
	}

	/**
	 * 优雅关闭：尽力发出 close_notify，然后执行后续关闭动作。
	 * <p>
	 * 以下情况会直接执行 {@code afterClose}，不再尝试发送 close_notify：
	 * 非 SSL 连接、握手未完成（对端还没建立会话，发通知意义不大）、通道已关闭、
	 * 抢占不到写状态（有在途写，直写会撞 {@code WritePendingException}）、产出记录失败。
	 * </p>
	 *
	 * @param channelContext 连接上下文
	 * @param afterClose     后续关闭动作（关闭 socket、投递关闭任务）。
	 *                       可能在当前线程同步执行，也可能在写完成回调里执行，实现需幂等
	 */
	public static void closeGracefully(ChannelContext channelContext, Runnable afterClose) {
		SslHandler sslHandler = channelContext.getSslHandler();
		AsynchronousSocketChannel socket = channelContext.asynchronousSocketChannel;
		if (sslHandler == null || !sslHandler.isHandshakeCompleted() || socket == null || !socket.isOpen()) {
			runAfterClose(channelContext, afterClose);
			return;
		}
		// 抢占写状态：单纯「先查 isWriting 再 write」中间有窗口，框架写线程可能恰好在此刻发起写，
		// 两边都调 write 就会撞 WritePendingException。抢占成功后框架的写任务会因 writing == true 让路。
		AbstractSendRunnable sendRunnable = channelContext.getSendRunnable();
		if (sendRunnable != null && !sendRunnable.tryStartWriting()) {
			log.debug("{}, 有在途写操作，跳过 TLS close_notify", channelContext);
			runAfterClose(channelContext, afterClose);
			return;
		}
		ByteBuffer closeNotify;
		try {
			// 让引擎产出 close_notify 记录（纯内存操作，不涉及 IO）
			closeNotify = sslHandler.closeGracefully();
		} catch (Throwable e) {
			log.warn("{}, 产出 TLS close_notify 失败:{}", channelContext, e.getMessage());
			finishWriting(sendRunnable);
			runAfterClose(channelContext, afterClose);
			return;
		}
		if (closeNotify == null) {
			finishWriting(sendRunnable);
			runAfterClose(channelContext, afterClose);
			return;
		}
		try {
			socket.write(new ByteBuffer[]{closeNotify}, 0, 1, CLOSE_NOTIFY_WRITE_TIMEOUT, TimeUnit.MILLISECONDS, null,
				new CloseNotifyHandler(channelContext, sendRunnable, closeNotify, afterClose));
		} catch (Throwable e) {
			log.warn("{}, 写入 TLS close_notify 失败:{}", channelContext, e.getMessage());
			finishWriting(sendRunnable);
			runAfterClose(channelContext, afterClose);
		}
	}

	/**
	 * 释放写状态
	 *
	 * @param sendRunnable 发送任务，可为 null
	 */
	private static void finishWriting(AbstractSendRunnable sendRunnable) {
		if (sendRunnable != null) {
			sendRunnable.finishWriting();
		}
	}

	/**
	 * 执行后续关闭动作，异常不外抛：关闭流程已经走到这一步，不能因为回调异常让连接卡在 waitingClose 状态
	 *
	 * @param channelContext 连接上下文
	 * @param afterClose     后续关闭动作
	 */
	private static void runAfterClose(ChannelContext channelContext, Runnable afterClose) {
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
		 * 待写出的 close_notify 记录，写完成后缓冲区 position 会前移，用于判断是否还有剩余
		 */
		private final ByteBuffer closeNotify;

		private CloseNotifyHandler(ChannelContext channelContext, AbstractSendRunnable sendRunnable,
								   ByteBuffer closeNotify, Runnable afterClose) {
			this.channelContext = channelContext;
			this.sendRunnable = sendRunnable;
			this.closeNotify = closeNotify;
			this.afterClose = afterClose;
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
				finishWriting(sendRunnable);
			} finally {
				runAfterClose(channelContext, afterClose);
			}
		}
	}
}
