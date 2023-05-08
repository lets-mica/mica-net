package org.tio.core.cluster.message;

/**
 * 集群 pong 消息
 *
 * @author L.cm
 */
public class ClusterPongMessage extends AbsClusterMessage {

	/**
	 * to ping 成员
	 */
	private final String pingMemberId;

	public ClusterPongMessage(String creatorMemberId, String pingMemberId) {
		super(creatorMemberId);
		this.pingMemberId = pingMemberId;
	}

	public String getPingMemberId() {
		return pingMemberId;
	}

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.PONG;
	}

}
