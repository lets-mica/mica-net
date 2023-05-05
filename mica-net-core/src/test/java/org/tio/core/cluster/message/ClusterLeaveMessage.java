package org.tio.core.cluster.message;

/**
 * 节点集群
 *
 * @author L.cm
 */
public class ClusterLeaveMessage extends AbsClusterMessage {

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.LEAVE;
	}

}
