package org.tio.core.cluster.message;

/**
 * 集群 pong 消息
 *
 * @author L.cm
 */
public class ClusterPongMessage extends AbsClusterMessage {

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.PONG;
	}

}
