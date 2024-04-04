package org.tio.server.cluster.codec;

import org.tio.core.ChannelContext;
import org.tio.core.Node;
import org.tio.server.cluster.message.*;
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
			case JOIN:
				return encodeJoinMessage((ClusterJoinMessage) message);
			default:
				throw new IllegalArgumentException("展示不支持该集群消息类型");
		}
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
		ByteBufferUtil.writeShortLE(buffer, payload.length);
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
		int capacity = 1 + 8 + 2 + payload.length;
		ByteBuffer buffer = ByteBuffer.allocate(capacity);
		buffer.put(ClusterMessageType.SYNC.getType());
		// 消息id
		long messageId = message.getMessageId();
		buffer.putLong(messageId);
		// 消息内容长度
		ByteBufferUtil.writeShortLE(buffer, payload.length);
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
		int capacity = 1 + 8;
		ByteBuffer buffer = ByteBuffer.allocate(capacity);
		buffer.put(ClusterMessageType.SYNC_ACK.getType());
		// 消息id
		buffer.putLong(message.getMessageId());
		return buffer;
	}

	/**
	 * 新加入消息
	 *
	 * @param message ClusterJoinMessage
	 * @return ByteBuffer
	 */
	private static ByteBuffer encodeJoinMessage(ClusterJoinMessage message) {
		int capacity = 1 + 2 + 32;
		ByteBuffer buffer = ByteBuffer.allocate(capacity);
		buffer.put(ClusterMessageType.JOIN.getType());
		Node joinMember = message.getJoinMember();
		// 端口，0到65535
		ByteBufferUtil.writeShortLE(buffer, joinMember.getPort());
		// ip 或者域名，预定长度为 32，考虑 ipv6（16）和长网址
		buffer.put(joinMember.getIp().getBytes(StandardCharsets.UTF_8));
		// 移到结尾
		buffer.position(capacity);
		return buffer;
	}

}
