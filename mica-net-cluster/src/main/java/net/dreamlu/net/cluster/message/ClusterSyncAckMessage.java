package net.dreamlu.net.cluster.message;

/**
 * 同步消息回复
 *
 * @author L.cm
 */
public class ClusterSyncAckMessage extends AbsClusterMessage {

	/**
	 * 消息Id
	 */
	private final long messageId;

	public ClusterSyncAckMessage(long messageId) {
		this.messageId = messageId;
	}

	public long getMessageId() {
		return messageId;
	}

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.SYNC_ACK;
	}

}
