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
	 * 新节点加入集群
	 */
	JOIN,
	/**
	 * 节点离开集群
	 */
	LEAVE,
	/**
	 * 数据同步
	 */
	SYNC,
	/**
	 * 数据同步回复
	 */
	SYNC_ACK,
	/**
	 * 数据
	 */
	DATA;

}
