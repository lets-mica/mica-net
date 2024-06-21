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
	 * @param message Cluster message to encode
	 * @return ByteBuffer with encoded bytes
	 */
	public ByteBuffer encode(AbsClusterMessage message) {
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
		int dataLength = payload.length;
		int dataLengthLength = getVariableLengthInt(dataLength);
		ByteBuffer buffer = ByteBuffer.allocate(1 + dataLengthLength + dataLength);
		buffer.put(ClusterMessageType.DATA.getType());
		// 消息内容长度
		writeVariableLengthInt(buffer, dataLength);
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
		int dataLength = payload.length;
		int dataLengthLength = getVariableLengthInt(dataLength);
		int capacity = 1 + 8 + dataLengthLength + dataLength;
		ByteBuffer buffer = ByteBuffer.allocate(capacity);
		buffer.put(ClusterMessageType.SYNC.getType());
		// 消息id
		long messageId = message.getMessageId();
		buffer.putLong(messageId);
		// 消息内容长度
		writeVariableLengthInt(buffer, dataLength);
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

	private static int getVariableLengthInt(int num) {
		int count = 0;
		do {
			num /= 128;
			count++;
		} while (num > 0);
		return count;
	}

	private static void writeVariableLengthInt(ByteBuffer buf, int num) {
		do {
			int digit = num % 128;
			num /= 128;
			if (num > 0) {
				digit |= 0x80;
			}
			buf.put((byte) digit);
		} while (num > 0);
	}

}
