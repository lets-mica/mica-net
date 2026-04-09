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

import net.dreamlu.mica.net.core.*;
import net.dreamlu.mica.net.core.ssl.SslFacadeContext;
import net.dreamlu.mica.net.core.task.AbstractDecodeRunnable;
import net.dreamlu.mica.net.core.task.HandlerRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * TCP specific ChannelContext - 仅 TCP 支持 SSL/TLS
 *
 * @author L.cm
 */
public abstract class TcpChannelContext extends ChannelContext {
	private static final Logger log = LoggerFactory.getLogger(TcpChannelContext.class);
	public AsynchronousSocketChannel asynchronousSocketChannel;
	public WriteCompletionHandler writeCompletionHandler;
	private ReadCompletionHandler readCompletionHandler;
	// TCP 专用 Runnable（实现基类抽象方法）
	private TcpDecodeRunnable decodeRunnable;
	private HandlerRunnable handlerRunnable;
	private TcpSendRunnable sendRunnable;
	private SslFacadeContext sslFacadeContext;

	public TcpChannelContext(TioConfig tioConfig, AsynchronousSocketChannel asynchronousSocketChannel) {
		super(tioConfig);
		this.asynchronousSocketChannel = asynchronousSocketChannel;
		initializeHandlers();
		initializeClientNode(asynchronousSocketChannel);
	}

	// Constructor for virtual contexts
	public TcpChannelContext(TioConfig tioConfig) {
		super(tioConfig);
		initializeHandlers();
	}

	public TcpChannelContext(TioConfig tioConfig, String id) {
		super(tioConfig, id);
		initializeHandlers();
	}

	/**
	 * Initialize read and write completion handlers
	 */
	private void initializeHandlers() {
		this.readCompletionHandler = new ReadCompletionHandler(this);
		this.writeCompletionHandler = new WriteCompletionHandler(this);
	}

	/**
	 * 设置 SSL/TLS（仅 TCP 支持）
	 */
	@Override
	public void setUpSSL() {
		if (tioConfig.sslConfig != null) {
			try {
				this.sslFacadeContext = new SslFacadeContext(this);
				if (tioConfig.isServer()) {
					this.sslFacadeContext.beginHandshake();
				}
			} catch (Exception e) {
				log.error("在开始SSL握手时发生了异常", e);
				Tio.close(this, "在开始SSL握手时发生了异常" + e.getMessage(), CloseCode.SSL_ERROR_ON_HANDSHAKE);
			}
		}
	}

	/**
	 * Initialize client node from AsynchronousSocketChannel
	 * This method unifies the duplicate logic from constructor and setter
	 */
	private void initializeClientNode(AsynchronousSocketChannel channel) {
		if (channel != null) {
			try {
				setClientNode(createClientNode(channel));
			} catch (IOException e) {
				assignAnUnknownClientNode();
			}
		} else {
			assignAnUnknownClientNode();
		}
	}

	/**
	 * Create client Node from AsynchronousSocketChannel
	 * This method is TCP-specific and implemented by subclasses
	 *
	 * @param asynchronousSocketChannel AsynchronousSocketChannel
	 * @return Node
	 * @throws IOException IOException
	 */
	protected abstract Node createClientNode(AsynchronousSocketChannel asynchronousSocketChannel) throws IOException;

	public ReadCompletionHandler getReadCompletionHandler() {
		return readCompletionHandler;
	}

	@Override
	public boolean isUdp() {
		return false;
	}

	/**
	 * TCP 专用：设置 TioConfig 并创建 TCP 专用的 Runnable
	 */
	@Override
	protected void setTioConfig(TioConfig tioConfig) {
		this.tioConfig = tioConfig;
		if (tioConfig != null) {
			// 创建 TCP 专用的 DecodeRunnable
			decodeRunnable = new TcpDecodeRunnable(this, tioConfig.tioExecutor);
			handlerRunnable = new HandlerRunnable(this, tioConfig.tioExecutor);
			// 创建 TCP 专用的 SendRunnable
			sendRunnable = new TcpSendRunnable(this, tioConfig.tioExecutor);
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
	public TcpSendRunnable getSendRunnable() {
		return sendRunnable;
	}

	@Override
	public SslFacadeContext getSslFacadeContext() {
		return sslFacadeContext;
	}

	public void setAsynchronousSocketChannel(AsynchronousSocketChannel asynchronousSocketChannel) {
		this.asynchronousSocketChannel = asynchronousSocketChannel;
		initializeClientNode(asynchronousSocketChannel);
	}
}
