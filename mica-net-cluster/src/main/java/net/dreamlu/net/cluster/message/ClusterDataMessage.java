package net.dreamlu.net.cluster.message;

/**
 * 集群 data 消息
 *
 * @author L.cm
 */
public class ClusterDataMessage extends AbsClusterMessage {

	/**
	 * 消息数据
	 */
	private final byte[] payload;

	public ClusterDataMessage(byte[] payload) {
		this.payload = payload;
	}

	public byte[] getPayload() {
		return payload;
	}

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.DATA;
	}

}
