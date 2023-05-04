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
	 * 新节点加入
	 */
	JOIN,
	/**
	 * 离开
	 */
	LEAVE,
	/**
	 * 数据
	 */
	DATA,
	;

}
