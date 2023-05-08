package org.tio.core.cluster.transport;

import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.cluster.codec.ClusterMessageDecoder;
import org.tio.core.cluster.codec.ClusterMessageEncoder;
import org.tio.core.cluster.message.AbsClusterMessage;
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
	private final ClusterMessageEncoder messageEncoder;
	private final ClusterMessageDecoder messageDecoder;

	public ClusterTcpServerHandler(ClusterMessageDecoder messageDecoder) {
		this.messageEncoder = ClusterMessageEncoder.INSTANCE;
		this.messageDecoder = messageDecoder;
	}

	@Override
	public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext context) throws TioDecodeException {
		return messageDecoder.decode(context, buffer, readableLength);
	}

	@Override
	public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext context) {
		return messageEncoder.encode(context, (AbsClusterMessage) packet);
	}

	@Override
	public void handler(Packet packet, ChannelContext context) throws Exception {

	}
}
