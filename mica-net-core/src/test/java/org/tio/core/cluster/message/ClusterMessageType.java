package org.tio.core.cluster.message;

/**
 * 集群消息类型
 *
 * @author L.cm
 */
public enum ClusterMessageType {

	/**
	 * 心跳消息 ping
	 */
	PING,
	/**
	 * 心跳回复消息 pong
	 */
	PONG,
	/**
	 * 新节点加入
	 */
	JOIN,
	/**
	 * 重连
	 */
	RECONNECT,
	/**
	 * 节点异常
	 */
	FAILED,
	/**
	 * 数据
	 */
	DATA,
	/**
	 * 数据同步
	 */
	SYNC,
	/**
	 * 数据同步回复
	 */
	SYNC_ACK;

}
