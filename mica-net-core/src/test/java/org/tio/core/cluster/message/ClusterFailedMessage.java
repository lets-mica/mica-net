package org.tio.core.cluster.message;

/**
 * 检测到失败的消息
 *
 * @author L.cm
 */
public class ClusterFailedMessage extends AbsClusterMessage {
	/**
	 * 失败的成员
	 */
	private final String failedMemberId;

	public ClusterFailedMessage(String failedMemberId) {
		this.failedMemberId = failedMemberId;
	}

	public String getFailedMemberId() {
		return failedMemberId;
	}

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.FAILED;
	}

}
