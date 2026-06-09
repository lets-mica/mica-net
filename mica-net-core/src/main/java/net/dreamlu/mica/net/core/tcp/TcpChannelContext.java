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
	/**
	 * 服务端 SSL 握手是否已触发。
	 * 仅对服务端有意义：setUpSSL() 阶段不再立即 beginHandshake，
	 * 留给 ReadCompletionHandler 在第一批数据到来时按需触发（兼容 ProxyProtocol 场景）。
	 */
	private boolean sslHandshakeStarted;

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
	 * 设置 SSL/TLS（仅 TCP 支持）。
	 * <p>
	 * 服务端不再立即 beginHandshake，而是等到 ReadCompletionHandler 第一次收到数据时
	 * 再触发握手，以兼容 SSL + ProxyProtocol 场景：ProxyProtocol 头是明文，必须先解析再走 SSL。
	 * 纯 SSL 场景下，ReadCompletionHandler 会在首次读取时调用 {@link #beginSslHandshakeIfNeeded()}，
	 * 行为与原先立即 beginHandshake 等价。
	 * </p>
	 */
	@Override
	public void setUpSSL() {
		if (tioConfig.sslConfig != null && sslFacadeContext == null) {
			try {
				this.sslFacadeContext = new SslFacadeContext(this);
				// 服务端延迟握手，触发点交由 ReadCompletionHandler
			} catch (Exception e) {
				log.error("在初始化SSL时发生了异常", e);
				Tio.close(this, "在初始化SSL时发生了异常" + e.getMessage(), CloseCode.SSL_ERROR_ON_HANDSHAKE);
			}
		}
	}

	/**
	 * 触发服务端 SSL 握手（幂等）。
	 * <p>
	 * 由 ReadCompletionHandler 在确认首批数据是 SSL ClientHello 时调用；
	 * 在 SSL + ProxyProtocol 场景下，需要先解析完代理头再调用本方法。
	 * </p>
	 *
	 * @throws Exception 握手启动异常
	 */
	public void beginSslHandshakeIfNeeded() throws Exception {
		if (sslFacadeContext != null && !sslHandshakeStarted && tioConfig.isServer()) {
			sslFacadeContext.beginHandshake();
			sslHandshakeStarted = true;
		}
	}

	/**
	 * 重置 SSL 握手标记（用于断线重连后重新触发握手）。
	 */
	public void resetSslHandshake() {
		this.sslHandshakeStarted = false;
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
