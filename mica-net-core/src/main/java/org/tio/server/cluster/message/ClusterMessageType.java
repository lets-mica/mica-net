package org.tio.server.cluster.message;

import org.tio.core.exception.TioDecodeException;

/**
 * 集群消息类型
 *
 * @author L.cm
 */
public enum ClusterMessageType {

	/**
	 * 心跳消息 ping
	 */
	PING((byte) 1),
	/**
	 * 心跳回复消息 pong
	 */
	PONG((byte) 2),
	/**
	 * 新节点加入
	 */
	JOIN((byte) 3),
	/**
	 * 重连
	 */
	RECONNECT((byte) 4),
	/**
	 * 节点异常
	 */
	FAILED((byte) 5),
	/**
	 * 数据
	 */
	DATA((byte) 6),
	/**
	 * 数据同步
	 */
	SYNC((byte) 7),
	/**
	 * 数据同步回复
	 */
	SYNC_ACK((byte) 8);

	private final byte type;

	ClusterMessageType(byte type) {
		this.type = type;
	}

	public byte getType() {
		return type;
	}

	/**
	 * Value of byte.
	 *
	 * @param value integer value
	 * @return command packet type enum
	 * @throws TioDecodeException Protocol decode exception
	 */
	public static ClusterMessageType from(final byte value) throws TioDecodeException {
		for (ClusterMessageType each : values()) {
			if (each.type == value) {
				return each;
			}
		}
		throw new TioDecodeException("Unsupported ClusterMessageType type:" + value);
	}

}
