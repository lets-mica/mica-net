package org.tio.core.cluster.transport;

import org.tio.client.intf.TioClientHandler;
import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.cluster.message.ClusterPingMessage;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;

import java.nio.ByteBuffer;

/**
 * 集群客户端处理器
 *
 * @author L.cm
 */
public class ClusterTcpClientHandler implements TioClientHandler {
	@Override
	public Packet heartbeatPacket(ChannelContext context) {
		return ClusterPingMessage.INSTANCE;
	}

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
