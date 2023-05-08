package org.tio.core.cluster.codec;

import org.tio.core.ChannelContext;
import org.tio.core.cluster.message.AbsClusterMessage;
import org.tio.core.cluster.message.ClusterMessageType;

import java.nio.ByteBuffer;

/**
 * 集群消息编码
 *
 * @author L.cm
 */
public class ClusterMessageEncoder {
	/**
	 * 单例
	 */
	public static final ClusterMessageEncoder INSTANCE = new ClusterMessageEncoder();

	private ClusterMessageEncoder() {
	}

	/**
	 * This is the main encoding method.
	 * It's only visible for testing.
	 *
	 * @param ctx       ChannelContext
	 * @param message   Cluster message to encode
	 * @return ByteBuffer with encoded bytes
	 */
	public ByteBuffer encode(ChannelContext ctx, AbsClusterMessage message) {
		ClusterMessageType messageType = message.getMessageType();
		switch (messageType) {
			case PING:
			case PONG:
				return encodePingPongMessage(messageType);
		}
		return ByteBuffer.allocate(0);
	}

	/**
	 * ping、pong 消息编码
	 *
	 * @param messageType ClusterMessageType
	 * @return ByteBuffer
	 */
	private static ByteBuffer encodePingPongMessage(ClusterMessageType messageType) {
		ByteBuffer buffer = ByteBuffer.allocate(1);
		buffer.put(messageType.getType());
		return buffer;
	}

}
