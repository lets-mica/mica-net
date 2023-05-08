package org.tio.core.cluster.message;

import org.tio.core.cluster.core.ClusterMember;

/**
 * 节点加入集群
 *
 * @author L.cm
 */
public class ClusterJoinMessage extends AbsClusterMessage {

	/**
	 * 加入的成员
	 */
	private final ClusterMember joinMember;

	public ClusterJoinMessage(ClusterMember joinMember) {
		this.joinMember = joinMember;
	}

	public ClusterMember getJoinMember() {
		return joinMember;
	}

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.JOIN;
	}

}
