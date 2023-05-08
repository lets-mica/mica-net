package org.tio.core.cluster.message;

/**
 * 同步消息回复
 *
 * @author L.cm
 */
public class ClusterSyncAckMessage extends AbsClusterMessage {

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.SYNC_ACK;
	}

}
