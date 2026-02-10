package org.tio.core.tcp;

import org.tio.core.ChannelContext;
import org.tio.core.Node;
import org.tio.core.ReadCompletionHandler;
import org.tio.core.TioConfig;
import org.tio.core.WriteCompletionHandler;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * TCP specific ChannelContext
 */
public abstract class TcpChannelContext extends ChannelContext {
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
