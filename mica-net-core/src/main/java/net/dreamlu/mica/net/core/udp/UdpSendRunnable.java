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

package net.dreamlu.mica.net.core.udp;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.Node;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.core.ssl.SslUtils;
import net.dreamlu.mica.net.core.task.AbstractSendRunnable;
import net.dreamlu.mica.net.core.utils.TioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * UDP 专用发送任务 - 简化的同步发送
 *
 * @author L.cm
 */
public class UdpSendRunnable extends AbstractSendRunnable {
	private static final Logger log = LoggerFactory.getLogger(UdpSendRunnable.class);

	public UdpSendRunnable(ChannelContext channelContext, Executor executor) {
		super(channelContext, executor);
	}

	public UdpSendRunnable(ChannelContext channelContext, Executor executor, Queue<Packet> msgQueue) {
		super(channelContext, executor, msgQueue);
	}

	/**
	 * UDP 逐个发送（每个 Packet 对应一个 UDP 数据报）
	 */
	@Override
	public void runTask() {
		Packet packet;
		while ((packet = msgQueue.poll()) != null) {
			sendPacket(packet, SslUtils.isSsl(tioConfig));
		}
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
		// UDP 现在只发送单个 Packet（不再批量发送）
		if (!(packets instanceof Packet)) {
			log.error("{}, UDP sendByteBuffer 期望 Packet 类型，实际为 {}", channelContext, packets.getClass().getName());
			return;
		}
		Packet packet = (Packet) packets;

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
		processAfterSent(packet, isSentSuccess);
	}

	/**
	 * 处理发送后的回调
	 */
	private void processAfterSent(Packet packet, boolean isSentSuccess) {
		channelContext.processAfterSent(packet, isSentSuccess);
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
}
