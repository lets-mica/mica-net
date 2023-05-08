package org.tio.core.cluster.message;

/**
 * 重连消息
 *
 * @author L.cm
 */
public class ClusterReconnectMessage extends AbsClusterMessage {
	/**
	 * 重连的成员Id
	 */
	private final String reconnectMemberId;

	public ClusterReconnectMessage(String reconnectMemberId) {
		this.reconnectMemberId = reconnectMemberId;
	}

	public String getReconnectMemberId() {
		return reconnectMemberId;
	}

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.RECONNECT;
	}
}
