package org.tio.core.tcp;

import org.tio.core.ChannelContext;
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
		this.readCompletionHandler = new ReadCompletionHandler(this);
		this.writeCompletionHandler = new WriteCompletionHandler(this);
		// Logic moved from ChannelContext.setAsynchronousSocketChannel
		if (asynchronousSocketChannel != null) {
			try {
				setClientNode(createClientNode(asynchronousSocketChannel));
			} catch (IOException e) {
				// Log and assign unknown
				assignAnUnknownClientNode();
			}
		} else {
			assignAnUnknownClientNode();
		}
	}

	// Constructor for virtual contexts
	public TcpChannelContext(TioConfig tioConfig) {
		super(tioConfig);
		this.readCompletionHandler = new ReadCompletionHandler(this);
		this.writeCompletionHandler = new WriteCompletionHandler(this);
	}

	public TcpChannelContext(TioConfig tioConfig, String id) {
		super(tioConfig, id);
		this.readCompletionHandler = new ReadCompletionHandler(this);
		this.writeCompletionHandler = new WriteCompletionHandler(this);
	}

	public ReadCompletionHandler getReadCompletionHandler() {
		return readCompletionHandler;
	}

	@Override
	public boolean isUdp() {
		return false;
	}

	public void setAsynchronousSocketChannel(AsynchronousSocketChannel asynchronousSocketChannel) {
		this.asynchronousSocketChannel = asynchronousSocketChannel;
		if (asynchronousSocketChannel != null) {
			try {
				setClientNode(createClientNode(asynchronousSocketChannel));
			} catch (IOException e) {
				assignAnUnknownClientNode();
			}
		} else {
			assignAnUnknownClientNode();
		}
	}
}
