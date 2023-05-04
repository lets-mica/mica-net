package org.tio.core.cluster;

/**
 * 集群消息类型
 *
 * @author L.cm
 */
public enum ClusterMessageType {

	/**
	 * 心跳消息
	 */
	HEARTBEAT,
	/**
	 * 新节点加入集群
	 */
	JOIN,
	/**
	 * 节点离开集群
	 */
	LEAVE,
	/**
	 * 集群数据同步
	 */
	SYNC,
	/**
	 * 数据
	 */
	DATA;

}
