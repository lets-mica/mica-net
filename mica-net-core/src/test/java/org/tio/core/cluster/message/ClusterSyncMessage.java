package org.tio.core.cluster.message;

/**
 * 同步消息
 *
 * @author L.cm
 */
public class ClusterSyncMessage extends ClusterDataMessage {
	/**
	 * 消息Id
	 */
	private final String messageId;

	public ClusterSyncMessage(String messageId, byte[] payload) {
		super(payload);
		this.messageId = messageId;
	}

	/**
	 * 转换成 ack 消息
	 *
	 * @return ack 消息
	 */
	public ClusterSyncAckMessage toAckMessage() {
		return new ClusterSyncAckMessage(this.messageId);
	}

	public String getMessageId() {
		return messageId;
	}

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.SYNC;
	}

}
