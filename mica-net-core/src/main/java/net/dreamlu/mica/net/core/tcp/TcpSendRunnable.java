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
import net.dreamlu.mica.net.core.WriteCompletionHandler;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.core.ssl.SslUtils;
import net.dreamlu.mica.net.core.task.AbstractSendRunnable;
import net.dreamlu.mica.net.core.utils.TioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
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
		if (writing.get() || msgQueue.isEmpty()) {
			return;
		}

		boolean isSsl = SslUtils.isSsl(tioConfig);
		int queueSize = msgQueue.size();

		// 单个包直接发送
		if (queueSize == 1) {
			Packet packet = msgQueue.poll();
			if (packet != null) {
				sendPacket(packet, isSsl);
			}
			return;
		}

		// 批量发送
		BatchEncodeResult result = batchEncode(queueSize, isSsl);
		if (result == null) {
			return;
		}

		// SSL 加密（如果需要，会合并为单个 ByteBuffer）
		ByteBuffer[] finalBuffers = encryptBatchIfNeeded(result.byteBuffers, result.packets, isSsl, result.needSslEncrypted);
		if (finalBuffers == null) {
			return; // 加密失败，已在方法内关闭连接
		}

		sendByteBuffers(finalBuffers, result.packets);
	}

	@Override
	public boolean sendPacket(Packet packet) {
		return sendPacket(packet, SslUtils.isSsl(tioConfig));
	}

	/**
	 * 发送单个数据包（TCP 异步）
	 */
	public boolean sendPacket(Packet packet, boolean isSsl) {
		ByteBuffer byteBuffer = getByteBuffer(packet);

		// SSL 加密
		byteBuffer = encryptIfNeeded(byteBuffer, packet, isSsl);
		if (byteBuffer == null) {
			return false; // 加密失败
		}

		sendByteBuffer(byteBuffer, packet);
		return true;
	}

	@Override
	protected void sendByteBuffer(ByteBuffer byteBuffer, Object packets) {
		if (byteBuffer == null) {
			log.error("{}, byteBuffer is null", channelContext);
			return;
		}

		if (!TioUtils.checkBeforeIO(channelContext)) {
			return;
		}

		// 标记写入状态
		writing.set(true);
		try {
			// 统一包装为 ByteBuffer[]{byteBuffer}，走 scatter-write 路径
			ByteBuffer[] buffers = new ByteBuffer[]{byteBuffer};
			WriteCompletionHandler.WriteCompletionVo writeCompletionVo = new WriteCompletionHandler.WriteCompletionVo(buffers, packets);
			TcpChannelContext tcpChannelContext = (TcpChannelContext) channelContext;
			tcpChannelContext.asynchronousSocketChannel.write(
				buffers, 0, 1,
				0L, TimeUnit.MILLISECONDS,
				writeCompletionVo, tcpChannelContext.writeCompletionHandler);
		} catch (Exception e) {
			// 发送失败，恢复状态
			writing.set(false);
			log.error("{}, TCP 异步写入失败", channelContext, e);
		}
	}

	/**
	 * 写操作完成回调（由 WriteCompletionHandler 调用）
	 * 清除写入标记并触发下一批消息发送
	 */
	public void onWriteCompleted() {
		writing.set(false);

		// 只在未提交状态且有消息时才触发，避免无效的 execute 调用和锁竞争
		// executed 为 true 表示任务已在线程池队列中，无需重复提交
		if (!msgQueue.isEmpty() && !this.executed) {
			this.execute();
		}
	}

	/**
	 * 检查是否有写操作正在进行
	 */
	public boolean isWriting() {
		return writing.get();
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
			return;
		}

		if (!TioUtils.checkBeforeIO(channelContext)) {
			return;
		}

		writing.set(true);
		try {
			TcpChannelContext tcpChannelContext = (TcpChannelContext) channelContext;
			// 统一走 scatter-write 路径，单个 buffer 也包装为数组
			WriteCompletionHandler.WriteCompletionVo writeCompletionVo = new WriteCompletionHandler.WriteCompletionVo(byteBuffers, packets);
			tcpChannelContext.asynchronousSocketChannel.write(
				byteBuffers, 0, byteBuffers.length,
				0L, TimeUnit.MILLISECONDS,
				writeCompletionVo, tcpChannelContext.writeCompletionHandler);
		} catch (Exception e) {
			// 发送失败，恢复状态
			writing.set(false);
			log.error("{}, TCP 批量发送失败", channelContext, e);
		}
	}
}
