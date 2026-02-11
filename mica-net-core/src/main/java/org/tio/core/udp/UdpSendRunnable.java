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
package org.tio.core.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.ChannelContext;
import org.tio.core.Node;
import org.tio.core.intf.Packet;
import org.tio.core.ssl.SslUtils;
import org.tio.core.task.AbstractSendRunnable;
import org.tio.core.utils.TioUtils;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * UDP 专用发送任务 - 简化的同步发送
 *
 * @author tanyaowu
 */
public class UdpSendRunnable extends AbstractSendRunnable {
	private static final Logger log = LoggerFactory.getLogger(UdpSendRunnable.class);

	public UdpSendRunnable(ChannelContext channelContext, Executor executor) {
		super(channelContext, executor);
	}

	public UdpSendRunnable(ChannelContext channelContext, Executor executor, Queue<Packet> msgQueue) {
		super(channelContext, executor, msgQueue);
	}

	@Override
	public void runTask() {
		if (msgQueue.isEmpty()) {
			return;
		}

		boolean isSsl = SslUtils.isSsl(tioConfig);
		int queueSize = msgQueue.size();

		// UDP 通常发送单个包（无连接，不适合批量）
		if (queueSize == 1) {
			Packet packet = msgQueue.poll();
			if (packet != null) {
				sendPacket(packet, isSsl);
			}
			return;
		}

		// 批量发送（虽然 UDP 不推荐批量，但支持以保持一致性）
		BatchEncodeResult result = batchEncode(queueSize, isSsl);
		if (result == null) {
			return;
		}

		// SSL 加密（UDP 通常使用 DTLS，这里简化处理）
		ByteBuffer[] buffers = encryptBatchIfNeeded(result.byteBuffers, result.packets, isSsl, result.needSslEncrypted);
		if (buffers == null) {
			return;
		}

		// UDP 不支持 gather write，需要合并 ByteBuffer
		ByteBuffer mergedBuffer = mergeBuffers(buffers);
		sendByteBuffer(mergedBuffer, result.packets);
	}

	@Override
	public boolean sendPacket(Packet packet) {
		return sendPacket(packet, SslUtils.isSsl(tioConfig));
	}

	/**
	 * 发送单个数据包（UDP 同步）
	 */
	public boolean sendPacket(Packet packet, boolean isSsl) {
		ByteBuffer byteBuffer = getByteBuffer(packet);

		// SSL 加密
		byteBuffer = encryptIfNeeded(byteBuffer, packet, isSsl);
		if (byteBuffer == null) {
			return false;
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

		boolean isSentSuccess = true;
		try {
			UdpChannelContext udpChannelContext = (UdpChannelContext) channelContext;
			if (udpChannelContext.datagramChannel != null) {
				if (udpChannelContext.datagramChannel.isConnected()) {
					// 已连接的 UDP，直接 write
					udpChannelContext.datagramChannel.write(byteBuffer);
				} else {
					// 未连接的 UDP，需要指定目标地址
					Node remoteNode = channelContext.isServer()
							? channelContext.getClientNode()
							: channelContext.getServerNode();
					udpChannelContext.datagramChannel.send(byteBuffer, remoteNode.getAsSocketAddress());
				}
			}
		} catch (Exception e) {
			log.error("{}, UDP 发送失败", channelContext, e);
			isSentSuccess = false;
		}

		// 后处理
		processAfterSent(packets, isSentSuccess);
	}

	/**
	 * 处理发送后的回调
	 */
	private void processAfterSent(Object packets, boolean isSentSuccess) {
		if (packets instanceof Packet) {
			channelContext.processAfterSent((Packet) packets, isSentSuccess);
		} else if (packets instanceof List) {
			@SuppressWarnings("unchecked")
			List<Packet> list = (List<Packet>) packets;
			for (Packet p : list) {
				channelContext.processAfterSent(p, isSentSuccess);
			}
		}
	}

	/**
	 * UDP 不支持 SSL/TLS 加密（需要 DTLS，当前未实现）
	 * 覆盖此方法以禁用 SSL 加密并记录警告日志
	 */
	@Override
	protected ByteBuffer encryptIfNeeded(ByteBuffer byteBuffer, Packet packet, boolean isSsl) {
		if (isSsl && tioConfig.sslConfig != null) {
			log.warn("{}, UDP 不支持 SSL/TLS 加密，需要使用 DTLS（当前未实现），将发送未加密数据", channelContext);
		}
		return byteBuffer;  // 直接返回原始数据，不加密
	}

	/**
	 * UDP 批量发送不支持 SSL/TLS 加密
	 * 覆盖此方法以禁用批量 SSL 加密
	 */
	@Override
	protected ByteBuffer[] encryptBatchIfNeeded(ByteBuffer[] byteBuffers, List<Packet> packets, boolean isSsl, boolean needSslEncrypted) {
		if (isSsl && needSslEncrypted && tioConfig.sslConfig != null) {
			log.warn("{}, UDP 批量发送不支持 SSL/TLS 加密，需要使用 DTLS（当前未实现）", channelContext);
		}
		return byteBuffers;  // 直接返回原始数据，不加密
	}

	/**
	 * 合并 ByteBuffer[] 为单个 ByteBuffer（UDP 不支持 gather write）
	 */
	private ByteBuffer mergeBuffers(ByteBuffer[] buffers) {
		if (buffers.length == 1) {
			return buffers[0];
		}

		int totalCapacity = 0;
		for (ByteBuffer buffer : buffers) {
			totalCapacity += buffer.remaining();
		}

		ByteBuffer merged = ByteBuffer.allocate(totalCapacity);
		for (ByteBuffer buffer : buffers) {
			merged.put(buffer);
		}
		merged.flip();
		return merged;
	}
}
