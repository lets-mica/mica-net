package org.tio.core.cluster.message;

/**
 * 同步消息
 *
 * @author L.cm
 */
public class ClusterSyncMessage extends AbsClusterMessage {
	/**
	 * 消息Id
	 */
	private final String messageId;
	/**
	 * 消息数据
	 */
	private byte[] payload;

	public ClusterSyncMessage(String creatorMemberId, String messageId) {
		super(creatorMemberId);
		this.messageId = messageId;
	}

	public String getMessageId() {
		return messageId;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.SYNC;
	}

}
