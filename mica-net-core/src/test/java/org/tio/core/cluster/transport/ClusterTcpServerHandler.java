package org.tio.core.cluster.transport;

import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.server.intf.TioServerHandler;

import java.nio.ByteBuffer;

/**
 * tcp 集群处理器
 *
 * @author L.cm
 */
public class ClusterTcpServerHandler implements TioServerHandler {
	@Override
	public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext context) throws TioDecodeException {
		return null;
	}

	@Override
	public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext context) {
		return null;
	}

	@Override
	public void handler(Packet packet, ChannelContext context) throws Exception {

	}
}
