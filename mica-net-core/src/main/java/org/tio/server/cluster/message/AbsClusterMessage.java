package org.tio.server.cluster.message;

import org.tio.core.intf.Packet;

/**
 * 集群消息类型
 *
 * @author L.cm
 */
public abstract class AbsClusterMessage extends Packet {

	/**
	 * 获取消息类型
	 *
	 * @return ClusterMessageType
	 */
	public abstract ClusterMessageType getMessageType();

	@Override
	public String toString() {
		return getMessageType().name();
	}
}
