package org.tio.core.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.ChannelContext;
import org.tio.core.Node;
import org.tio.core.ReadCompletionHandler;
import org.tio.core.Tio;
import org.tio.core.TioConfig;
import org.tio.core.WriteCompletionHandler;
import org.tio.core.ssl.SslFacadeContext;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * TCP specific ChannelContext - 仅 TCP 支持 SSL/TLS
 */
public abstract class TcpChannelContext extends ChannelContext {
	private static final Logger log = LoggerFactory.getLogger(TcpChannelContext.class);

	public AsynchronousSocketChannel asynchronousSocketChannel;
	public WriteCompletionHandler writeCompletionHandler;
	private ReadCompletionHandler readCompletionHandler;

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

	public void setAsynchronousSocketChannel(AsynchronousSocketChannel asynchronousSocketChannel) {
		this.asynchronousSocketChannel = asynchronousSocketChannel;
		initializeClientNode(asynchronousSocketChannel);
	}
}
