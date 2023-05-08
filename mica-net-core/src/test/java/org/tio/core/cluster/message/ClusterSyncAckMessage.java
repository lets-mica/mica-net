package org.tio.core.cluster.message;

/**
 * 同步消息回复
 *
 * @author L.cm
 */
public class ClusterSyncAckMessage extends AbsClusterMessage {

	/**
	 * 消息Id
	 */
	private final String messageId;

	public ClusterSyncAckMessage(String creatorMemberId, String messageId) {
		super(creatorMemberId);
		this.messageId = messageId;
	}

	public String getMessageId() {
		return messageId;
	}

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.SYNC_ACK;
	}

}
