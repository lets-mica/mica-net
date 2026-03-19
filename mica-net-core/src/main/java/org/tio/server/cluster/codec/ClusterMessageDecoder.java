/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & www.dreamlu.net).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tio.server.cluster.codec;

import org.tio.core.ChannelContext;
import org.tio.core.Node;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.server.cluster.message.*;
import org.tio.utils.buffer.ByteBufferUtil;
import org.tio.utils.json.JsonUtil;
import org.tio.utils.mica.Pair;

import java.nio.ByteBuffer;
import java.util.Map;

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
		buffer.mark();
		if (readableLength < 1 + 8) {
			buffer.reset();
			return null;
		}
		long timestamp = buffer.getLong();
		Pair<Integer, Integer> headersLengthPair = readDataPacketLength(buffer);
		if (headersLengthPair == null) {
			buffer.reset();
			return null;
		}
		int headersLength = headersLengthPair.getLeft();
		int messageLength = 1 + 8 + headersLengthPair.getRight() + headersLength;
		if (readableLength < messageLength) {
			ctx.setPacketNeededLength(messageLength);
			return null;
		}
		Map<String, String> headers = decodeHeaders(buffer, headersLength);
		Pair<Integer, Integer> dataLengthPair = readDataPacketLength(buffer);
		if (dataLengthPair == null) {
			buffer.reset();
			return null;
		}
		int dataLength = dataLengthPair.getLeft();
		int dataLengthLength = dataLengthPair.getRight();
		messageLength = 1 + 8 + headersLengthPair.getRight() + headersLength + dataLengthLength + dataLength;
		if (readableLength < messageLength) {
			ctx.setPacketNeededLength(messageLength);
			return null;
		}
		byte[] payload = ByteBufferUtil.readBytes(buffer, dataLength);
		return new ClusterDataMessage(timestamp, headers, payload);
	}

	/**
	 * 同步数据消息解码
	 *
	 * @param buffer         buffer
	 * @param readableLength 可读长度
	 * @return ClusterSyncMessage
	 */
	private static ClusterSyncMessage decodeSyncMessage(ChannelContext ctx, ByteBuffer buffer, int readableLength) {
		if (readableLength < 1 + 8 + 8) {
			return null;
		}
		long messageId = buffer.getLong();
		long timestamp = buffer.getLong();
		Pair<Integer, Integer> headersLengthPair = readDataPacketLength(buffer);
		if (headersLengthPair == null) {
			return null;
		}
		int headersLength = headersLengthPair.getLeft();
		int headersLengthLength = headersLengthPair.getRight();
		int messageLength = 1 + 8 + 8 + headersLengthLength + headersLength;
		if (readableLength < messageLength) {
			ctx.setPacketNeededLength(messageLength);
			return null;
		}
		Map<String, String> headers = decodeHeaders(buffer, headersLength);
		Pair<Integer, Integer> dataLengthPair = readDataPacketLength(buffer);
		if (dataLengthPair == null) {
			return null;
		}
		int dataLength = dataLengthPair.getLeft();
		int dataLengthLength = dataLengthPair.getRight();
		messageLength = 1 + 8 + 8 + headersLengthLength + headersLength + dataLengthLength + dataLength;
		if (readableLength < messageLength) {
			ctx.setPacketNeededLength(messageLength);
			return null;
		}
		byte[] payload = ByteBufferUtil.readBytes(buffer, dataLength);
		return new ClusterSyncMessage(messageId, timestamp, headers, payload);
	}

	/**
	 * 数据消息解码
	 *
	 * @param buffer         buffer
	 * @param readableLength 可读长度
	 * @return ClusterSyncAckMessage
	 */
	private static ClusterSyncAckMessage decodeSyncAckMessage(ChannelContext ctx, ByteBuffer buffer, int readableLength) {
		int packetLength = 1 + 8;
		// 消息不够读
		if (readableLength < packetLength) {
			ctx.setPacketNeededLength(packetLength);
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

	private static Pair<Integer, Integer> readDataPacketLength(ByteBuffer buffer) {
		int remainingLength = 0;
		int multiplier = 1;
		short digit;
		int count = 0;
		do {
			if (!buffer.hasRemaining()) {
				return null;
			}
			digit = ByteBufferUtil.readUnsignedByte(buffer);
			remainingLength += (digit & 127) * multiplier;
			multiplier *= 128;
			count++;
		} while ((digit & 128) != 0 && count < 4);
		return new Pair<>(remainingLength, count);
	}

	private static Map<String, String> decodeHeaders(ByteBuffer buffer, int headersLength) {
		if (headersLength == 0) {
			return null;
		}
		byte[] headersBytes = ByteBufferUtil.readBytes(buffer, headersLength);
		return JsonUtil.readMap(headersBytes, String.class, String.class);
	}

}
