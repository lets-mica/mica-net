package net.dreamlu.net.cluster.message;

/**
 * 同步消息
 *
 * @author L.cm
 */
public class ClusterSyncMessage extends ClusterDataMessage {
	/**
	 * 消息Id
	 */
	private final long messageId;

	public ClusterSyncMessage(long messageId, byte[] payload) {
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

	public long getMessageId() {
		return messageId;
	}

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.SYNC;
	}

}
