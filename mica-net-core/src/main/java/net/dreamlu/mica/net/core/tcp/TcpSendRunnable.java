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

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.WriteCompletionHandler.WriteCompletionVo;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.core.ssl.SslUtils;
import net.dreamlu.mica.net.core.task.AbstractSendRunnable;
import net.dreamlu.mica.net.core.utils.TioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCP 专用发送任务 - 支持异步发送和批量优化
 *
 * @author L.cm
 */
public class TcpSendRunnable extends AbstractSendRunnable {
	private static final Logger log = LoggerFactory.getLogger(TcpSendRunnable.class);

	/**
	 * TCP 专用：标记是否有写操作正在进行
	 * 用于防止 AsynchronousSocketChannel 的 WritePendingException
	 */
	private final AtomicBoolean writing = new AtomicBoolean(false);

	public TcpSendRunnable(ChannelContext channelContext, Executor executor) {
		super(channelContext, executor);
	}

	public TcpSendRunnable(ChannelContext channelContext, Executor executor, Queue<Packet> msgQueue) {
		super(channelContext, executor, msgQueue);
	}

	@Override
	public void runTask() {
		// 如果有写操作正在进行，直接返回，避免 WritePendingException
		if (writing.get()) {
			return;
		}

		Packet firstPacket = msgQueue.poll();
		if (firstPacket == null) {
			return;
		}

		// 判断是否启用 SSL
		boolean isSsl = SslUtils.isSsl(tioConfig);
		// SSL 握手未完成时，业务包不能加密，暂存起来等握手完成后统一发出
		if (deferIfSslHandshakeNotCompleted(firstPacket)) {
			deferRemainingPackets();
			return;
		}
		// 单个包直接发送
		if (msgQueue.isEmpty()) {
			sendPacket(firstPacket, isSsl);
			return;
		}

		// 批量发送
		BatchEncodeResult result = batchEncode(firstPacket, isSsl);
		if (result == null) {
			return;
		}

		// SSL 加密（如果需要，会合并为单个 ByteBuffer）
		ByteBuffer[] finalBuffers = encryptBatchIfNeeded(result.byteBuffers, isSsl, result.needSslEncrypted);
		if (finalBuffers == null) {
			// 加密失败，已在方法内关闭连接；这批包没发出去，通知发送方
			notifySendFailed(result.packets);
			return;
		}

		// 批量发送 ByteBuffer[]
		sendByteBuffers(finalBuffers, result.packets);
	}

	/**
	 * 把队列里剩余的业务包也暂存起来（SSL 握手未完成时统一等待握手完成后发出）。
	 */
	private void deferRemainingPackets() {
		Packet packet;
		while ((packet = msgQueue.poll()) != null) {
			if (!deferIfSslHandshakeNotCompleted(packet)) {
				// 不需要加密的包无法暂存，放回队列，下轮再处理
				msgQueue.add(packet);
				break;
			}
		}
	}

	@Override
	public boolean sendPacket(Packet packet) {
		return sendPacket(packet, SslUtils.isSsl(tioConfig));
	}

	/**
	 * 发送单个数据包（TCP 异步）
	 */
	@Override
	public boolean sendPacket(Packet packet, boolean isSsl) {
		// SSL 握手未完成时不能加密业务数据，暂存起来等握手完成后统一发出。
		// 直接加密会拿到握手记录而不是业务密文，对端收到的是非法 TLS 记录。
		if (deferIfSslHandshakeNotCompleted(packet)) {
			return true;
		}
		ByteBuffer byteBuffer = getByteBuffer(packet);
		if (byteBuffer == null) {
			// 编码失败，已在 getByteBuffer 内通知发送方
			return false;
		}

		// SSL 加密
		byteBuffer = encryptIfNeeded(byteBuffer, packet, isSsl);
		if (byteBuffer == null) {
			// 加密失败，连接已被关闭，这个包没发出去，通知发送方
			notifySendFailed(packet);
			return false;
		}

		// 发送 ByteBuffer
		sendByteBuffer(byteBuffer, packet);
		return true;
	}

	@Override
	protected void sendByteBuffer(ByteBuffer byteBuffer, Object packets) {
		if (byteBuffer == null) {
			log.error("{}, byteBuffer is null", channelContext);
			notifySendFailed(packets);
			return;
		}
		if (!TioUtils.checkBeforeIO(channelContext)) {
			// 连接已在关闭中，这个包发不出去了，通知发送方，
			// 否则等待发送完成的业务方只能空耗到超时
			notifySendFailed(packets);
			return;
		}
		// 抢占写状态再发起写：write() 不允许并发，AIO 在已有未完成写时会抛 WritePendingException。
		// 抢不到说明有写在途（框架的上一批，或关闭流程正在直写 close_notify），此时不能写，
		// 把包放回队列，由在途写的完成回调（onWriteCompleted）继续调度。
		if (!writing.compareAndSet(false, true)) {
			requeueOnWriteConflict(packets);
			return;
		}
		try {
			// 统一包装为 ByteBuffer[]{byteBuffer}，走 scatter-write 路径
			ByteBuffer[] buffers = new ByteBuffer[]{byteBuffer};
			WriteCompletionVo writeCompletionVo = new WriteCompletionVo(buffers, packets);
			channelContext.asynchronousSocketChannel.write(
				buffers, 0, 1,
				0L, TimeUnit.MILLISECONDS,
				writeCompletionVo, channelContext.writeCompletionHandler);
		} catch (Exception e) {
			// 发送失败，恢复状态
			writing.set(false);
			log.error("{}, TCP 异步写入失败", channelContext, e);
		}
	}

	/**
	 * 有写在途时把没发出去的包放回队列。
	 * <p>
	 * 两种在途写要区分处理：
	 * <ul>
	 *   <li>连接正在关闭（关闭流程直写 close_notify）：包已经没机会发出去了，直接丢弃；</li>
	 *   <li>框架自己的另一批写：包放回队列即可，那次写的完成回调会重新调度发送。</li>
	 * </ul>
	 * 包只能加到队尾（{@code ConcurrentLinkedQueue} 没有 addFirst），这里不做顺序补偿：
	 * 走到这条分支说明本端已经有一次写在途——要么是直发路径（{@code useQueueSend=false}
	 * 或 SSL 握手记录）自己发起的，要么是关闭流程正在直写 close_notify。
	 * 前者本来就没有 FIFO 保证（多个线程并发直发谁先谁不定），
	 * 后者连接本来就要关了，顺序无所谓。
	 * </p>
	 *
	 * @param packets 单个 {@link Packet} 或 {@code List<Packet>}
	 */
	private void requeueOnWriteConflict(Object packets) {
		if (channelContext.isWaitingClose() || channelContext.isClosed() || channelContext.isRemoved()) {
			log.debug("{}, 连接正在关闭，丢弃未发出的包: {}", channelContext, packets);
			// 包发不出去了，通知发送方，否则等待发送完成的业务方只能空耗到超时
			notifySendFailed(packets);
			return;
		}
		if (packets instanceof Packet) {
			msgQueue.add((Packet) packets);
		} else if (packets instanceof Collection) {
			for (Object packet : (Collection<?>) packets) {
				if (packet instanceof Packet) {
					msgQueue.add((Packet) packet);
				}
			}
		}
		log.debug("{}, 有写在途，未发出的包已重新入队: {}", channelContext, packets);
	}

	/**
	 * 写操作完成回调（由 WriteCompletionHandler 调用）
	 * 清除写入标记并触发下一批消息发送
	 */
	@Override
	public void onWriteCompleted() {
		writing.set(false);
		// 在途写结束后执行挂起的回调（如 TLS close_notify 抢不到写状态时挂的重试）
		Runnable callback = afterWriteCallback.getAndSet(null);
		if (callback != null) {
			try {
				callback.run();
			} catch (Throwable e) {
				log.error("{}, 在途写结束后的回调执行失败", channelContext, e);
			}
		}
		// 只在未提交状态且有消息时才触发，避免无效的 execute 调用和锁竞争
		// executed 为 true 表示任务已在线程池队列中，无需重复提交
		resumeSend();
	}

	/**
	 * 抢占写状态（CAS），所有发起写的地方都用它，避免与框架写路径撞 {@code WritePendingException}
	 *
	 * @return true 表示抢占成功
	 */
	@Override
	public boolean tryStartWriting() {
		return writing.compareAndSet(false, true);
	}

	/**
	 * 释放 {@link #tryStartWriting()} 抢占的写状态
	 */
	@Override
	public void finishWriting() {
		writing.set(false);
	}

	@Override
	protected boolean isWriting() {
		return writing.get();
	}

	/**
	 * 重置 writing 状态，用于重连场景。
	 * <p>
	 * 当 write 操作挂起（网络中断/超时）导致连接被关闭时，
	 * writing 可能仍为 true，若不重置，重连后 runTask() 会因
	 * writing.get() == true 直接 return，导致消息（如 MQTT CONNECT）永远发不出去。
	 *
	 * @see TcpCloseRunnable 连接关闭前的重置
	 */
	public void resetWriting() {
		writing.set(false);
	}

	/**
	 * 批量发送 ByteBuffer[] - 使用 gather write 零拷贝发送
	 * <p>
	 * 对于多个 ByteBuffer，直接使用 AsynchronousSocketChannel.write(ByteBuffer[], ...)
	 * 让 OS 内核进行分散聚集写入，无需预先合并，避免内存拷贝。
	 */
	protected void sendByteBuffers(ByteBuffer[] byteBuffers, Object packets) {
		if (byteBuffers == null || byteBuffers.length == 0) {
			log.error("{}, byteBuffers is null or empty", channelContext);
			notifySendFailed(packets);
			return;
		}

		if (!TioUtils.checkBeforeIO(channelContext)) {
			// 连接已在关闭中，这批包发不出去了，通知发送方
			notifySendFailed(packets);
			return;
		}

		// 同 sendByteBuffer：先 CAS 抢占写状态，抢不到就把整批包放回队列
		if (!writing.compareAndSet(false, true)) {
			requeueOnWriteConflict(packets);
			return;
		}
		try {
			// 统一走 scatter-write 路径，单个 buffer 也包装为数组
			WriteCompletionVo writeCompletionVo = new WriteCompletionVo(byteBuffers, packets);
			channelContext.asynchronousSocketChannel.write(
				byteBuffers, 0, byteBuffers.length,
				0L, TimeUnit.MILLISECONDS,
				writeCompletionVo, channelContext.writeCompletionHandler);
		} catch (Exception e) {
			// 发送失败，恢复状态
			writing.set(false);
			log.error("{}, TCP 批量发送失败", channelContext, e);
		}
	}
}
