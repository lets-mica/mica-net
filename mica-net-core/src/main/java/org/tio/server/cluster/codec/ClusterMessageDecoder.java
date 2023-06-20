package org.tio.server.cluster.codec;

import org.tio.core.ChannelContext;
import org.tio.core.Node;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.server.cluster.message.*;
import org.tio.utils.buffer.ByteBufferUtil;

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
		// 消息长度不够读
		if (readableLength < 1) {
			return null;
		}
		ClusterMessageType messageType = ClusterMessageType.from(buffer.get());
		switch (messageType) {
			case PING:
				return ClusterPingMessage.INSTANCE;
			case PONG:
				return ClusterPongMessage.INSTANCE;
			case DATA:
				return decodeDataMessage(ctx, buffer, readableLength);
			case SYNC:
				return decodeSyncMessage(ctx, buffer, readableLength);
			case SYNC_ACK:
				return decodeSyncAckMessage(ctx, buffer, readableLength);
			case JOIN:
				return decodeJoinMessage(ctx, buffer, readableLength);
			default:
				throw new TioDecodeException("暂不支持的集群消息类型");
		}
	}

	/**
	 * 异步数据消息解码
	 *
	 * @param ctx            ChannelContext
	 * @param buffer         buffer
	 * @param readableLength 可读长度
	 * @return ClusterDataMessage
	 */
	private static ClusterDataMessage decodeDataMessage(ChannelContext ctx, ByteBuffer buffer, int readableLength) {
		// 消息不够读
		if (readableLength < 3) {
			return null;
		}
		int dataLength = ByteBufferUtil.readUnsignedShortLE(buffer);
		int messageLength = 3 + dataLength;
		if (readableLength < messageLength) {
			ctx.setPacketNeededLength(messageLength);
			return null;
		}
		// 数据
		byte[] payload = ByteBufferUtil.readBytes(buffer, dataLength);
		return new ClusterDataMessage(payload);
	}

	/**
	 * 同步数据消息解码
	 *
	 * @param buffer         buffer
	 * @param readableLength 可读长度
	 * @return ClusterSyncMessage
	 */
	private static ClusterSyncMessage decodeSyncMessage(ChannelContext ctx, ByteBuffer buffer, int readableLength) {
		// 消息不够读
		if (readableLength < 11) {
			return null;
		}
		// 消息 id
		long messageId = buffer.getLong();
		int dataLength = ByteBufferUtil.readUnsignedShortLE(buffer);
		int messageLength = 11 + dataLength;
		if (readableLength < messageLength) {
			ctx.setPacketNeededLength(messageLength);
			return null;
		}
		// 数据
		byte[] payload = ByteBufferUtil.readBytes(buffer, dataLength);
		return new ClusterSyncMessage(messageId, payload);
	}

	/**
	 * 数据消息解码
	 *
	 * @param buffer         buffer
	 * @param readableLength 可读长度
	 * @return ClusterSyncAckMessage
	 */
	private static ClusterSyncAckMessage decodeSyncAckMessage(ChannelContext ctx, ByteBuffer buffer, int readableLength) {
		// 消息不够读
		if (readableLength < 9) {
			ctx.setPacketNeededLength(9);
			return null;
		}
		// 消息 id
		long messageId = buffer.getLong();
		return new ClusterSyncAckMessage(messageId);
	}

	/**
	 * 新加入消息解码
	 *
	 * @param buffer         buffer
	 * @param readableLength 可读长度
	 * @return ClusterSyncAckMessage
	 */
	private static ClusterJoinMessage decodeJoinMessage(ChannelContext ctx, ByteBuffer buffer, int readableLength) {
		int messageLength = 1 + 2 + 32;
		// 消息不够读
		if (readableLength < messageLength) {
			ctx.setPacketNeededLength(messageLength);
			return null;
		}
		// 端口号
		int port = ByteBufferUtil.readUnsignedShortLE(buffer);
		// ip
		String ip = ByteBufferUtil.readString(buffer, 32);
		return new ClusterJoinMessage(new Node(ip.trim(), port));
	}

}
