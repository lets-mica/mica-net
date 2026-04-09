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
import net.dreamlu.mica.net.core.Tio;
import net.dreamlu.mica.net.core.TioConfig;
import net.dreamlu.mica.net.core.ssl.SslFacadeContext;
import net.dreamlu.mica.net.core.task.AbstractDecodeRunnable;
import net.dreamlu.mica.net.core.task.AbstractSendRunnable;
import net.dreamlu.mica.net.core.task.HandlerRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * UDP 专用 ChannelContext
 *
 * @author L.cm
 */
public class UdpChannelContext extends ChannelContext {
	private static final Logger log = LoggerFactory.getLogger(UdpChannelContext.class);
	public DatagramChannel datagramChannel;
	// UDP 专用 Runnable（实现基类抽象方法）
	private UdpDecodeRunnable decodeRunnable;
	private HandlerRunnable handlerRunnable;
	private UdpSendRunnable sendRunnable;

	public UdpChannelContext(TioConfig tioConfig, DatagramChannel datagramChannel, Node remoteNode) {
		super(tioConfig);
		this.datagramChannel = datagramChannel;
		if (remoteNode != null) {
			this.setClientNode(remoteNode);
		}
		this.setClosed(false);
	}

	/**
	 * Protected constructor for subclasses (e.g., UdpClientChannelContext)
	 * that need to set the client node after construction
	 */
	protected UdpChannelContext(TioConfig tioConfig, DatagramChannel datagramChannel) {
		super(tioConfig);
		this.datagramChannel = datagramChannel;
		this.setClosed(false);
	}

	/**
	 * Handle received UDP data by decoding it
	 * This method unifies the duplicate logic from UdpClient and UdpServer
	 *
	 * @param buffer the received data buffer
	 */
	public void handleReceivedData(ByteBuffer buffer) {
		if (tioConfig.useQueueDecode) {
			decodeRunnable.addMsg(buffer);
			decodeRunnable.execute();
		} else {
			decodeRunnable.setNewReceivedByteBuffer(buffer);
			decodeRunnable.decode();
		}
	}

	/**
	 * Handle UDP read errors
	 *
	 * @param e       the throwable
	 * @param message error message
	 */
	public void handleReadError(Throwable e, String message) {
		log.error(message, e);
		Tio.close(this, e, message, CloseCode.READ_ERROR);
	}

	@Override
	public boolean isServer() {
		return tioConfig.isServer();
	}

	@Override
	public void setUpSSL() {
		// 暂不支持 SSL/TLS
	}

	@Override
	public boolean isUdp() {
		return true;
	}

	/**
	 * UDP 专用：设置 TioConfig 并创建 UDP 专用的 Runnable
	 */
	@Override
	protected void setTioConfig(TioConfig tioConfig) {
		this.tioConfig = tioConfig;
		if (tioConfig != null) {
			// 创建 UDP 专用的 DecodeRunnable
			decodeRunnable = new UdpDecodeRunnable(this, tioConfig.tioExecutor);
			handlerRunnable = new HandlerRunnable(this, tioConfig.tioExecutor);
			// 创建 UDP 专用的 SendRunnable
			sendRunnable = new UdpSendRunnable(this, tioConfig.tioExecutor);
			tioConfig.connections.add(this);
		}
	}

	@Override
	public AbstractDecodeRunnable getDecodeRunnable() {
		return decodeRunnable;
	}

	@Override
	public HandlerRunnable getHandlerRunnable() {
		return handlerRunnable;
	}

	@Override
	public AbstractSendRunnable getSendRunnable() {
		return sendRunnable;
	}

	@Override
	public SslFacadeContext getSslFacadeContext() {
		// UDP 不支持 SSL
		return null;
	}
}
