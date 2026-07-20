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
import java.util.Queue;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.ChannelContext.CloseCode;
import net.dreamlu.mica.net.core.Tio;
import net.dreamlu.mica.net.core.TioConfig;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.core.intf.TioListener;
import net.dreamlu.mica.net.core.task.AbstractSendRunnable;
import net.dreamlu.mica.net.utils.buffer.ByteBufferUtil;

/**
 * Connection-level SSL/TLS handler.
 *
 * @author L.cm
 */
public final class SslHandler {
	private static final Logger log = LoggerFactory.getLogger(SslHandler.class);

	private final ChannelContext channelContext;
	private final SslEngineWorker engineWorker;

	public SslHandler(ChannelContext channelContext) {
		this(channelContext, channelContext.tioConfig.sslConfig);
	}

	public SslHandler(ChannelContext channelContext, SslConfig sslConfig) {
		this.channelContext = channelContext;
		SSLContext sslContext = sslConfig.getSslContext();
		this.engineWorker = new SslEngineWorker(channelContext, sslContext, !channelContext.isServer(), sslConfig,
			this::sendEncryptedData, this::onPlainData, this::onHandshakeCompleted, this::onSessionClosed);
	}

	public void beginHandshake() throws SSLException {
		log.info("{} 开始SSL握手", channelContext);
		engineWorker.beginHandshake();
	}

	public boolean isHandshakeCompleted() {
		return engineWorker.isHandshakeCompleted();
	}

	public ByteBuffer encrypt(ByteBuffer plainData) throws SSLException {
		return engineWorker.encrypt(plainData);
	}

	public void decrypt(ByteBuffer encryptedData) throws SSLException {
		engineWorker.decrypt(encryptedData);
	}

	public void close() {
		engineWorker.close();
	}

	public void terminate() {
		engineWorker.terminate();
	}

	private void sendEncryptedData(ByteBuffer encryptedData) {
		if (log.isDebugEnabled()) {
			log.debug("{}, 发送SSL协议数据，{}", channelContext, encryptedData);
		}
		Packet packet = new Packet();
		packet.setPreEncodedByteBuffer(encryptedData);
		packet.setSslEncrypted(true);
		if (channelContext.tioConfig.useQueueSend) {
			boolean added = channelContext.getSendRunnable().addMsg(packet);
			if (added) {
				channelContext.getSendRunnable().execute();
			}
		} else {
			channelContext.getSendRunnable().sendPacket(packet, true);
		}
	}

	private void onPlainData(ByteBuffer plainData) {
		if (!isHandshakeCompleted()) {
			log.debug("{}, SSL握手尚未完成，忽略解密数据: {}", channelContext, plainData);
			return;
		}
		if (channelContext.tioConfig.useQueueDecode) {
			ByteBuffer copiedByteBuffer = ByteBufferUtil.copy(plainData);
			channelContext.getDecodeRunnable().addMsg(copiedByteBuffer);
			channelContext.getDecodeRunnable().execute();
		} else {
			channelContext.getDecodeRunnable().setNewReceivedByteBuffer(plainData);
			channelContext.getDecodeRunnable().decode();
		}
	}

	private void onHandshakeCompleted() {
		log.info("{}, 完成SSL握手", channelContext);
		TioConfig tioConfig = channelContext.tioConfig;
		TioListener tioListener = tioConfig.getTioListener();
		if (tioListener != null) {
			try {
				tioListener.onAfterConnected(channelContext, true, channelContext.isReconnect());
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		AbstractSendRunnable sendRunnable = channelContext.getSendRunnable();
		Queue<Packet> pendingPackets = sendRunnable.getForSendAfterSslHandshakeCompleted(false);
		if (pendingPackets == null || pendingPackets.isEmpty()) {
			return;
		}
		if (log.isDebugEnabled()) {
			log.debug("{} 业务层在SSL握手前有{}条数据待发送", channelContext, pendingPackets.size());
		}
		boolean useQueueSend = tioConfig.useQueueSend;
		Packet packet;
		while ((packet = pendingPackets.poll()) != null) {
			if (useQueueSend) {
				sendRunnable.addMsg(packet);
			} else {
				sendRunnable.sendPacket(packet, true);
			}
		}
		if (useQueueSend) {
			sendRunnable.execute();
		}
	}

	private void onSessionClosed() {
		Tio.close(channelContext, null, "SSL SessionClosed", channelContext.isServer(), CloseCode.SSL_SESSION_CLOSED);
	}
}
