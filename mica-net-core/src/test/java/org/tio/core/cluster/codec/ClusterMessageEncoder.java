package org.tio.core.cluster.codec;

import org.tio.core.ChannelContext;
import org.tio.core.cluster.message.*;
import org.tio.utils.buffer.ByteBufferUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

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
	 * @param ctx     ChannelContext
	 * @param message Cluster message to encode
	 * @return ByteBuffer with encoded bytes
	 */
	public ByteBuffer encode(ChannelContext ctx, AbsClusterMessage message) {
		ClusterMessageType messageType = message.getMessageType();
		switch (messageType) {
			case PING:
			case PONG:
				return encodePingPongMessage(messageType);
			case DATA:
				return encodeDataMessage((ClusterDataMessage) message);
			case SYNC:
				return encodeSyncMessage((ClusterSyncMessage) message);
			case SYNC_ACK:
				return encodeSyncAckMessage((ClusterSyncAckMessage) message);
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

	/**
	 * 异步的数据消息
	 *
	 * @param message ClusterDataMessage
	 * @return ByteBuffer
	 */
	private static ByteBuffer encodeDataMessage(ClusterDataMessage message) {
		byte[] payload = message.getPayload();
		ByteBuffer buffer = ByteBuffer.allocate(1 + 2 + payload.length);
		buffer.put(ClusterMessageType.DATA.getType());
		// 消息内容长度
		ByteBufferUtil.writeUnsignedShort(buffer, payload.length);
		buffer.put(payload);
		return buffer;
	}

	/**
	 * 同步的数据消息
	 *
	 * @param message ClusterSyncMessage
	 * @return ByteBuffer
	 */
	private static ByteBuffer encodeSyncMessage(ClusterSyncMessage message) {
		byte[] payload = message.getPayload();
		int capacity = 1 + 21 + 2 + payload.length;
		ByteBuffer buffer = ByteBuffer.allocate(capacity);
		buffer.put(ClusterMessageType.SYNC.getType());
		// 消息id
		String messageId = message.getMessageId();
		buffer.put(messageId.getBytes(StandardCharsets.UTF_8));
		// 消息内容长度
		ByteBufferUtil.writeUnsignedShort(buffer, payload.length);
		buffer.put(payload);
		return buffer;
	}

	/**
	 * 同步的数据消息回复
	 *
	 * @param message ClusterSyncAckMessage
	 * @return ByteBuffer
	 */
	private static ByteBuffer encodeSyncAckMessage(ClusterSyncAckMessage message) {
		int capacity = 1 + 21;
		ByteBuffer buffer = ByteBuffer.allocate(capacity);
		buffer.put(ClusterMessageType.SYNC_ACK.getType());
		// 消息id
		String messageId = message.getMessageId();
		buffer.put(messageId.getBytes(StandardCharsets.UTF_8));
		return buffer;
	}

}
