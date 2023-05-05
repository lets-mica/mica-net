package org.tio.core.cluster.message;

/**
 * 集群 ping 消息
 *
 * @author L.cm
 */
public class ClusterPingMessage extends AbsClusterMessage {

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.PING;
	}

}
