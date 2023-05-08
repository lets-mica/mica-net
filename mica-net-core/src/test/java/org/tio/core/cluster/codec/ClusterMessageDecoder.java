package org.tio.core.cluster.codec;

import org.tio.core.ChannelContext;
import org.tio.core.cluster.message.ClusterMessageType;
import org.tio.core.cluster.message.ClusterPingMessage;
import org.tio.core.cluster.message.ClusterPongMessage;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;

import java.nio.ByteBuffer;

/**
 * 集群消息解码
 */
public class ClusterMessageDecoder {

	/**
	 * @param ctx            ChannelContext
	 * @param buffer         ByteBuffer
	 * @param readableLength readableLength
	 * @return Packet
	 * @throws TioDecodeException TioDecodeException
	 */
	public Packet decode(ChannelContext ctx, ByteBuffer buffer, int readableLength) throws TioDecodeException {
		ClusterMessageType messageType = ClusterMessageType.from(buffer.get());
		switch (messageType) {
			case PING:
				return ClusterPingMessage.INSTANCE;
			case PONG:
				return ClusterPongMessage.INSTANCE;
		}
		return null;
	}

}
