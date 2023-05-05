package org.tio.core.cluster.message;

/**
 * 节点同步消息
 *
 * @author L.cm
 */
public class ClusterSyncMessage extends AbsClusterMessage {

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.SYNC;
	}

}
