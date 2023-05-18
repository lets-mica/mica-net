package net.dreamlu.net.cluster.message;

/**
 * 集群 pong 消息
 *
 * @author L.cm
 */
public class ClusterPongMessage extends AbsClusterMessage {
	/**
	 * 实例
	 */
	public static final ClusterPongMessage INSTANCE = new ClusterPongMessage();

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.PONG;
	}

}
