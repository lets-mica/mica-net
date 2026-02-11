

package org.tio.core.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.ChannelContext;
import org.tio.core.ChannelContext.CloseCode;
import org.tio.core.Tio;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.core.task.AbstractDecodeRunnable;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

/**
 * UDP 专用解码任务 - 简化的单次解码（无流式拼接、无慢包检测）
 *
 * @author L.cm
 */
public class UdpDecodeRunnable extends AbstractDecodeRunnable {
	private static final Logger log = LoggerFactory.getLogger(UdpDecodeRunnable.class);

	public UdpDecodeRunnable(ChannelContext channelContext, Executor executor) {
		super(channelContext, executor);
	}

	/**
	 * 清空处理的队列消息
	 */
	@Override
	public void clearMsgQueue() {
		super.clearMsgQueue();
		newReceivedByteBuffer = null;
	}

	@Override
	public void runTask() {
		while ((newReceivedByteBuffer = msgQueue.poll()) != null) {
			decode();
		}
	}

	/**
	 * UDP 解码：单次解码，数据报完整或丢失
	 * UDP 不需要流式拼接、不需要慢包检测、不支持 ProxyProtocol
	 */
	@Override
	public void decode() {
		ByteBuffer byteBuffer = newReceivedByteBuffer;

		try {
			int initPosition = byteBuffer.position();
			int limit = byteBuffer.limit();
			int readableLength = limit - initPosition;

			// UDP 单次解码（数据报完整或丢失，不存在半包）
			Packet packet = tioHandler.decode(byteBuffer, limit, initPosition, readableLength, channelContext);

			if (packet == null) {
				// UDP 解码失败 = 数据报损坏或协议错误，直接丢弃
				if (log.isDebugEnabled()) {
					log.debug("{} UDP 数据报解码失败，丢弃 {} 字节数据", channelContext, readableLength);
				}
				return;
			}

			// 解码成功
			int packetSize = byteBuffer.position() - initPosition;
			onDecodeSuccess(packet, packetSize);

			// UDP 数据报解码后通常不会有剩余数据（除非协议设计允许）
			if (byteBuffer.hasRemaining()) {
				if (log.isWarnEnabled()) {
					log.warn("{} UDP 数据报解码后还剩 {} 字节数据，可能存在协议问题",
						channelContext, byteBuffer.remaining());
				}
			}

		} catch (TioDecodeException e) {
			// 解码异常，记录日志后丢弃数据报
			if (channelContext.isLogWhenDecodeError()) {
				log.error("{} UDP 解码异常，丢弃数据报", channelContext, e);
			}
			// UDP 解码失败通常不关闭连接（无连接协议），直接丢弃即可
			channelContext.stat.decodeFailCount++;
		} catch (Throwable e) {
			// 其他严重异常，关闭连接
			if (channelContext.isLogWhenDecodeError()) {
				log.error("{} UDP 解码时遇到严重异常", channelContext, e);
			}
			Tio.close(channelContext, e, "UDP解码异常:" + e.getMessage(), CloseCode.DECODE_ERROR);
		}
	}
}
