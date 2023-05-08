package org.tio.core.cluster.message;

import org.tio.core.cluster.core.ClusterMember;

/**
 * 节点加入集群
 *
 * @author L.cm
 */
public class ClusterJoinMessage extends AbsClusterMessage {
	/**
	 * 触发的成员 Id
	 */
	private final String creatorMemberId;
	/**
	 * 加入的成员
	 */
	private final ClusterMember joinMember;

	public ClusterJoinMessage(String creatorMemberId, ClusterMember joinMember) {
		this.creatorMemberId = creatorMemberId;
		this.joinMember = joinMember;
	}

	public String getCreatorMemberId() {
		return creatorMemberId;
	}

	public ClusterMember getJoinMember() {
		return joinMember;
	}

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.JOIN;
	}

}
