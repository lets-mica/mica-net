package net.dreamlu.net.cluster.message;

import org.tio.core.Node;

/**
 * 节点加入集群
 *
 * @author L.cm
 */
public class ClusterJoinMessage extends AbsClusterMessage {

	/**
	 * 加入的成员
	 */
	private final Node joinMember;

	public ClusterJoinMessage(Node joinMember) {
		this.joinMember = joinMember;
	}

	public Node getJoinMember() {
		return joinMember;
	}

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.JOIN;
	}

}
