package org.tio.core.cluster.message;

/**
 * 检测到失败的消息
 *
 * @author L.cm
 */
public class ClusterFailedMessage extends AbsClusterMessage {
	/**
	 * 触发的成员 Id
	 */
	private final String creatorMemberId;
	/**
	 * 失败的成员
	 */
	private final String failedMemberId;

	public ClusterFailedMessage(String creatorMemberId, String failedMemberId) {
		this.creatorMemberId = creatorMemberId;
		this.failedMemberId = failedMemberId;
	}

	public String getCreatorMemberId() {
		return creatorMemberId;
	}

	public String getFailedMemberId() {
		return failedMemberId;
	}

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.FAILED;
	}

}
