package org.tio.core.cluster;

import org.tio.core.intf.Packet;

/**
 * 集群消息类型
 *
 * @author L.cm
 */
public class ClusterMessage extends Packet {
	/**
	 * 消息类型
	 */
	private ClusterMessageType messageType;
	/**
	 * 消息数据
	 */
	private byte[] payload;

	// 1. 1 byte 消息类型
	// 2. 32 位字符串 messageId
	// 3. data 需要自定义序列化

}
