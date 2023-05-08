package org.tio.core.cluster.message;

/**
 * 重连消息
 *
 * @author L.cm
 */
public class ClusterReconnectMessage extends AbsClusterMessage {
	/**
	 * 触发的成员 Id
	 */
	private final String creatorMemberId;
	/**
	 * 重连的成员Id
	 */
	private final String reconnectMemberId;

	public ClusterReconnectMessage(String creatorMemberId, String reconnectMemberId) {
		this.creatorMemberId = creatorMemberId;
		this.reconnectMemberId = reconnectMemberId;
	}

	public String getCreatorMemberId() {
		return creatorMemberId;
	}

	public String getReconnectMemberId() {
		return reconnectMemberId;
	}

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.RECONNECT;
	}
}
