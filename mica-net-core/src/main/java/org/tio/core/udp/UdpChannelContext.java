/*
    Apache License
    Version 2.0, January 2004
    http://www.apache.org/licenses/
*/
package org.tio.core.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.ChannelContext;
import org.tio.core.Node;
import org.tio.core.Tio;
import org.tio.core.TioConfig;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * @author tanyaowu
 */
public class UdpChannelContext extends ChannelContext {
	private static final Logger log = LoggerFactory.getLogger(UdpChannelContext.class);
	public DatagramChannel datagramChannel;

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
	 * @param e the throwable
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
}
