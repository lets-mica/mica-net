package org.tio.core.cluster.message;

/**
 * 集群 ping 消息
 *
 * @author L.cm
 */
public class ClusterPingMessage extends AbsClusterMessage {
	/**
	 * 实例
	 */
	public static final ClusterPingMessage INSTANCE = new ClusterPingMessage();

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.PING;
	}

}
