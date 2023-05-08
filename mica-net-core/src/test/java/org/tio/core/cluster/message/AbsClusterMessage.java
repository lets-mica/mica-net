package org.tio.core.cluster.message;

import org.tio.core.intf.Packet;

/**
 * 集群消息类型
 *
 * @author L.cm
 */
public abstract class AbsClusterMessage extends Packet {
	/**
	 * 触发的成员 Id
	 */
	private final String creatorMemberId;

	public AbsClusterMessage(String creatorMemberId) {
		this.creatorMemberId = creatorMemberId;
	}

	public String getCreatorMemberId() {
		return creatorMemberId;
	}

	/**
	 * 获取消息类型
	 *
	 * @return ClusterMessageType
	 */
	public abstract ClusterMessageType getMessageType();

}
