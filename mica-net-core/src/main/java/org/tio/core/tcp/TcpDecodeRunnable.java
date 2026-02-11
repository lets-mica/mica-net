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

package org.tio.core.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.ChannelContext;
import org.tio.core.ChannelContext.CloseCode;
import org.tio.core.Tio;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.core.stat.ChannelStat;
import org.tio.core.stat.SlowPacketDetector;
import org.tio.core.task.AbstractDecodeRunnable;
import org.tio.server.proxy.ProxyProtocolDecoder;
import org.tio.utils.buffer.ByteBufferUtil;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

/**
 * TCP 专用解码任务 - 支持流式拼接、慢包检测、ProxyProtocol
 *
 * @author L.cm
 */
public class TcpDecodeRunnable extends AbstractDecodeRunnable {
	private static final Logger log = LoggerFactory.getLogger(TcpDecodeRunnable.class);
	private static final int MIN_AVERAGE_BYTES_THRESHOLD = 256;
	private static final int BUFFER_SIZE_RATIO = 2;

	/**
	 * 上一次解码剩下的数据（TCP 流式协议专用）
	 */
	private ByteBuffer lastByteBuffer = null;

	public TcpDecodeRunnable(ChannelContext channelContext, Executor executor) {
		super(channelContext, executor);
	}

	/**
	 * 清空处理的队列消息
	 */
	@Override
	public void clearMsgQueue() {
		super.clearMsgQueue();
		lastByteBuffer = null;
		newReceivedByteBuffer = null;
	}

	@Override
	public void runTask() {
		while ((newReceivedByteBuffer = msgQueue.poll()) != null) {
			decode();
		}
	}

	/**
	 * TCP 解码：支持流式拼接、循环解包、慢包攻击检测
	 */
	@Override
	public void decode() {
		boolean isServer = tioConfig.isServer();
		ByteBuffer byteBuffer = newReceivedByteBuffer;

		// TCP 流式拼接：将上次未完成的数据与本次数据合并
		if (lastByteBuffer != null) {
			byteBuffer = ByteBufferUtil.composite(lastByteBuffer, byteBuffer);
			lastByteBuffer = null;
		}

		// 循环解包（TCP 一次 read 可能包含多个完整包）
		while (true) {
			try {
				int initPosition = byteBuffer.position();
				int limit = byteBuffer.limit();
				int readableLength = limit - initPosition;
				Packet packet = null;

				// 解包需要的包长
				Integer packetNeededLength = channelContext.packetNeededLength;
				if (packetNeededLength != null) {
					if (log.isDebugEnabled()) {
						log.debug("{}, 解码所需长度:{}", channelContext, packetNeededLength);
					}
					if (readableLength >= packetNeededLength) {
						packet = decodePacket(isServer, byteBuffer, limit, initPosition, readableLength);
					}
				} else {
					try {
						packet = decodePacket(isServer, byteBuffer, limit, initPosition, readableLength);
					} catch (BufferUnderflowException e) {
						//数据不够读
					}
				}

				// 数据不够，解不了码
				if (packet == null) {
					if (tioConfig.useQueueDecode || (byteBuffer != newReceivedByteBuffer)) {
						byteBuffer.position(initPosition);
						byteBuffer.limit(limit);
						lastByteBuffer = byteBuffer;
					} else {
						lastByteBuffer = ByteBufferUtil.copy(byteBuffer, initPosition, limit);
					}
					ChannelStat channelStat = channelContext.stat;
					channelStat.decodeFailCount++;

					// 慢包攻击检测（TCP 专用）
					if (tioConfig.enableSlowPacketDetection && channelStat.decodeFailCount > 0) {
						channelStat.getSlowPacketDetector(tioConfig.slowPacketWindowSize, tioConfig.slowPacketCheckInterval)
							.recordReceive(readableLength);
					}

					if (log.isDebugEnabled()) {
						log.debug("{} 本次解码失败, 已经连续{}次解码失败，参与解码的数据长度共{}字节", channelContext, channelStat.decodeFailCount, readableLength);
					}

					// 慢包攻击检测（TCP 专用）
					if (tioConfig.enableSlowPacketDetection && channelStat.decodeFailCount > tioConfig.maxDecodeFailCount) {
						if (packetNeededLength == null) {
							if (log.isInfoEnabled()) {
								log.info("{} 本次解码失败, 已经连续{}次解码失败，参与解码的数据长度共{}字节", channelContext, channelStat.decodeFailCount, readableLength);
							}
						}

						// 使用滑动窗口计算平均值（如果已初始化）
						int avgBytes;
						if (channelStat.hasSlowPacketDetector()) {
							SlowPacketDetector detector = channelStat.getSlowPacketDetector(tioConfig.slowPacketWindowSize, tioConfig.slowPacketCheckInterval);
							// 检查是否需要本次检测（降低检测频率）
							if (!detector.shouldCheck(channelStat.decodeFailCount)) {
								return;
							}
							avgBytes = detector.getAverageBytes();
						} else {
							// 回退到原始算法（兼容禁用检测器的情况）
							avgBytes = readableLength / channelStat.decodeFailCount;
						}

						int threshold = Math.min(channelContext.getReadBufferSize() / BUFFER_SIZE_RATIO, MIN_AVERAGE_BYTES_THRESHOLD);
						if (avgBytes < threshold) {
							// 构造异常信息
							String str = "连续解码" + channelStat.decodeFailCount + "次都不成功，" +
								"参与解码的数据长度共" + readableLength + "字节，";
							if (packetNeededLength != null) {
								str += "解码所需长度" + packetNeededLength + "字节，";
							}
							// 检查慢包攻击
							str += "并且平均每次接收到的数据为" + avgBytes + "字节，有慢攻击的嫌疑";
							// 打印报文结构，方便定位问题
							String hexDump = ByteBufferUtil.hexDump(lastByteBuffer);
							log.error("{} {}，报文结构：\n{}", channelContext, str, hexDump);
							// 抛出解码异常
							throw new TioDecodeException(str);
						}
					}
					return;
				} else {
					// 解码成功
					int packetSize = byteBuffer.position() - initPosition;
					onDecodeSuccess(packet, packetSize);

					// 组包后，还剩有数据
					if (byteBuffer.hasRemaining()) {
						if (log.isDebugEnabled()) {
							log.debug("{},组包后，还剩有数据:{}", channelContext, byteBuffer.remaining());
						}
					} else {
						// 组包后，数据刚好用完
						lastByteBuffer = null;
						if (log.isDebugEnabled()) {
							log.debug("{},组包后，数据刚好用完", channelContext);
						}
						return;
					}
				}
			} catch (Throwable e) {
				if (channelContext.isLogWhenDecodeError()) {
					log.error("解码时遇到异常", e);
				}
				channelContext.setPacketNeededLength(null);
				this.lastByteBuffer = null;
				Tio.close(channelContext, e, "解码异常:" + e.getMessage(), CloseCode.DECODE_ERROR);
				return;
			}
		}
	}

	/**
	 * TCP 专用解码：支持 ProxyProtocol
	 */
	private Packet decodePacket(boolean isServer, ByteBuffer byteBuffer, int limit, int initPosition, int readableLength) throws TioDecodeException {
		if (isServer) {
			// TCP Server 支持 ProxyProtocol v1（用于 nginx/ELB 转发）
			return ProxyProtocolDecoder.decodeIfEnable(channelContext, byteBuffer, readableLength, (context, buffer, readableLen) ->
				tioHandler.decode(buffer, limit, initPosition, readableLen, context)
			);
		} else {
			return tioHandler.decode(byteBuffer, limit, initPosition, readableLength, channelContext);
		}
	}
}
