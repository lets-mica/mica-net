/*
    Apache License
    Version 2.0, January 2004
    http://www.apache.org/licenses/
*/
package org.tio.core.udp;

import org.tio.core.ChannelContext;
import org.tio.core.Node;
import org.tio.core.TioConfig;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.DatagramChannel;

/**
 * @author tanyaowu
 */
public class UdpChannelContext extends ChannelContext {

    public java.nio.channels.DatagramChannel datagramChannel;

	public UdpChannelContext(TioConfig tioConfig, DatagramChannel datagramChannel, Node remoteNode) {
		super(tioConfig);
		this.datagramChannel = datagramChannel;
		this.setClientNode(remoteNode);
		this.setClosed(false);
	}

	@Override
	public Node createClientNode(AsynchronousSocketChannel asynchronousSocketChannel) throws IOException {
		return createUnknownNode();
	}

	@Override
	public boolean isServer() {
		return tioConfig.isServer();
	}

    @Override
    public boolean isUdp() {
        return true;
    }
}
