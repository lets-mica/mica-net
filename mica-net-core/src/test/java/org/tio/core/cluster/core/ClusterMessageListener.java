package org.tio.core.cluster.core;

import org.tio.core.cluster.message.ClusterDataMessage;

/**
 * 集群消息监听器
 *
 * @author L.cm
 */
@FunctionalInterface
public interface ClusterMessageListener {

	/**
	 * 集群消息监听
	 *
	 * @param message ClusterDataMessage
	 */
	void onMessage(ClusterDataMessage message);

}
