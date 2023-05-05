package org.tio.core.cluster.message;

/**
 * 节点加入集群
 *
 * @author L.cm
 */
public class ClusterJoinMessage extends AbsClusterMessage {

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.JOIN;
	}

}
