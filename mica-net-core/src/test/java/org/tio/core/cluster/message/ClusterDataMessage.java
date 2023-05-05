package org.tio.core.cluster.message;

/**
 * 集群 data 消息
 *
 * @author L.cm
 */
public class ClusterDataMessage extends AbsClusterMessage {

	/**
	 * 消息数据
	 */
	private byte[] payload;

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.DATA;
	}

}
