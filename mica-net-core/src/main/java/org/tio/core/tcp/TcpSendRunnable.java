/*
	Apache License
	Version 2.0, January 2004
	http://www.apache.org/licenses/

	Copyright 2020 t-io

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	  http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
package org.tio.core.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.ChannelContext;
import org.tio.core.WriteCompletionHandler.WriteCompletionVo;
import org.tio.core.intf.Packet;
import org.tio.core.ssl.SslUtils;
import org.tio.core.task.AbstractSendRunnable;
import org.tio.core.utils.TioUtils;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCP 专用发送任务 - 支持异步发送和批量优化
 *
 * @author tanyaowu
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
			WriteCompletionVo writeCompletionVo = new WriteCompletionVo(byteBuffer, packets);
			TcpChannelContext tcpChannelContext = (TcpChannelContext) channelContext;

			// 异步发送，不阻塞当前线程
			tcpChannelContext.asynchronousSocketChannel.write(
					byteBuffer,
					writeCompletionVo,
					tcpChannelContext.writeCompletionHandler);
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
	 * 批量发送 ByteBuffer[] - 循环调用单个 write 避免预先合并的内存拷贝
	 */
	protected void sendByteBuffers(ByteBuffer[] byteBuffers, Object packets) {
		if (byteBuffers == null || byteBuffers.length == 0) {
			log.error("{}, byteBuffers is null or empty", channelContext);
			return;
		}

		if (!TioUtils.checkBeforeIO(channelContext)) {
			return;
		}

		// 方案2：循环发送每个 ByteBuffer，避免预先合并
		// 优势：延迟合并到真正需要时，减少内存拷贝；保持异步发送
		// 标记写入状态
		writing.set(true);
		try {
			TcpChannelContext tcpChannelContext = (TcpChannelContext) channelContext;

			if (byteBuffers.length == 1) {
				// 单个 ByteBuffer，直接发送
				WriteCompletionVo writeCompletionVo = new WriteCompletionVo(byteBuffers[0], packets);
				tcpChannelContext.asynchronousSocketChannel.write(byteBuffers[0], writeCompletionVo, tcpChannelContext.writeCompletionHandler);
			} else {
				// 多个 ByteBuffer，合并后发送（避免多次异步调用的开销）
				int totalCapacity = 0;
				for (ByteBuffer buffer : byteBuffers) {
					totalCapacity += buffer.remaining();
				}
				ByteBuffer mergedBuffer = ByteBuffer.allocate(totalCapacity);
				for (ByteBuffer buffer : byteBuffers) {
					mergedBuffer.put(buffer);
				}
				mergedBuffer.flip();

				WriteCompletionVo writeCompletionVo = new WriteCompletionVo(mergedBuffer, packets);
				tcpChannelContext.asynchronousSocketChannel.write(mergedBuffer, writeCompletionVo, tcpChannelContext.writeCompletionHandler);
			}
		} catch (Exception e) {
			// 发送失败，恢复状态
			writing.set(false);
			log.error("{}, TCP 批量发送失败", channelContext, e);
		}
	}
}
