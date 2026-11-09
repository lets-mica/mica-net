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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.task.AbstractSendRunnable;

/**
 * TLS 优雅关闭：在关闭 socket 之前把 TLS close_notify 记录发出去，让对端感知到是本端主动断开。
 * <p>
 * 为什么不能走发送队列：连接关闭流程里，发送任务已经被取消、socket 也即将关闭，
 * 此时把 close_notify 塞进发送队列只会得到两个结果——被 {@code addMsg} 的取消判断直接丢弃，
 * 或者被 {@code checkBeforeIO} 的 waitingClose 判断拦下，两条路都发不出去。
 * 所以这里绕过发送队列，直接把记录写进 socket。
 * </p>
 *
 * @author L.cm
 */
public final class SslGracefulShutdown {
	private static final Logger log = LoggerFactory.getLogger(SslGracefulShutdown.class);
	/**
	 * 等待在途写操作结束的最长时间（毫秒）
	 */
	private static final long WAIT_WRITING_TIMEOUT = 20L;
	/**
	 * 等待 close_notify 写完成的最长时间（毫秒）
	 */
	private static final long WAIT_WRITE_TIMEOUT = 100L;
	/**
	 * 轮询在途写状态的间隔（毫秒）
	 */
	private static final long WRITING_POLL_INTERVAL = 1L;

	private SslGracefulShutdown() {
	}

	/**
	 * 发送 TLS close_notify。
	 *
	 * @param channelContext 连接上下文
	 * @return true 表示 close_notify 已成功写入 socket
	 */
	public static boolean sendCloseNotify(ChannelContext channelContext) {
		SslHandler sslHandler = channelContext.getSslHandler();
		if (sslHandler == null || !sslHandler.isHandshakeCompleted()) {
			// 握手未完成：对端可能还没建立会话，发 close_notify 意义不大
			return false;
		}
		// 1. 抢占写状态：单纯「先查 isWriting 再 write」中间有窗口，
		//    框架写线程可能恰好在此刻发起写，两边都调 write 就会撞 WritePendingException。
		//    抢占成功后框架的写任务会因 writing == true 主动让路。
		if (!acquireWriting(channelContext, WAIT_WRITING_TIMEOUT)) {
			log.warn("{}, 写操作未结束，跳过 TLS close_notify", channelContext);
			return false;
		}
		try {
			// 2. 让引擎产出 close_notify 记录
			ByteBuffer closeNotify = sslHandler.closeGracefully();
			if (closeNotify == null) {
				return false;
			}
			// 3. 直接写 socket 并等待写完成，确保 socket 关闭前数据已经交给内核
			return writeAndWait(channelContext, closeNotify, WAIT_WRITE_TIMEOUT);
		} finally {
			releaseWriting(channelContext);
		}
	}

	/**
	 * 抢占写状态。
	 * <p>
	 * 已有在途写时短暂重试：关闭流程里在途写通常很快结束（回调失败或完成），
	 * 抢占成功才能保证「本次写」与框架写路径互斥。
	 * </p>
	 *
	 * @param channelContext 连接上下文
	 * @param timeout        最长等待时间（毫秒）
	 * @return true 表示抢占成功
	 */
	private static boolean acquireWriting(ChannelContext channelContext, long timeout) {
		AbstractSendRunnable sendRunnable = channelContext.getSendRunnable();
		if (sendRunnable == null) {
			return true;
		}
		long deadline = System.currentTimeMillis() + timeout;
		while (!sendRunnable.tryStartWriting()) {
			if (!sendRunnable.isWriting()) {
				// CAS 失败且当前没有写操作在进行：说明写状态由不支持抢占的实现持有，按「无在途写」处理
				return true;
			}
			if (System.currentTimeMillis() >= deadline) {
				return false;
			}
			try {
				Thread.sleep(WRITING_POLL_INTERVAL);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return true;
	}

	/**
	 * 释放写状态
	 *
	 * @param channelContext 连接上下文
	 */
	private static void releaseWriting(ChannelContext channelContext) {
		AbstractSendRunnable sendRunnable = channelContext.getSendRunnable();
		if (sendRunnable != null) {
			sendRunnable.finishWriting();
		}
	}

	/**
	 * 直接把数据写入 socket 并等待写完成。
	 * <p>
	 * 这里用 {@code write(ByteBuffer[])} 而不是 {@code write(ByteBuffer)}：框架的写路径统一走
	 * {@code write(ByteBuffer[])}（回调值类型为 Long），同一个 channel 上混用两种写 API 容易出类型冲突。
	 * </p>
	 *
	 * @param channelContext 连接上下文
	 * @param data           待写数据
	 * @param timeout        最长等待时间（毫秒）
	 * @return true 表示写成功
	 */
	private static boolean writeAndWait(ChannelContext channelContext, ByteBuffer data, long timeout) {
		AsynchronousSocketChannel socket = channelContext.asynchronousSocketChannel;
		if (socket == null || !socket.isOpen()) {
			return false;
		}
		CountDownLatch latch = new CountDownLatch(1);
		AtomicBoolean success = new AtomicBoolean(false);
		try {
			socket.write(new ByteBuffer[]{data}, 0, 1, 0L, TimeUnit.MILLISECONDS, null,
				new CompletionHandler<Long, Object>() {
					@Override
					public void completed(Long result, Object attachment) {
						success.set(result != null && result > 0);
						latch.countDown();
					}

					@Override
					public void failed(Throwable exc, Object attachment) {
						latch.countDown();
					}
				});
		} catch (Throwable e) {
			log.warn("{}, 写入 TLS close_notify 失败:{}", channelContext, e.getMessage());
			return false;
		}
		try {
			if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
				log.warn("{}, 等待 TLS close_notify 写出超时", channelContext);
				return false;
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
		return success.get();
	}
}
