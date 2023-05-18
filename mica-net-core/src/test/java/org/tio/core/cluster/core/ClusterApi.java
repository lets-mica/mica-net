package org.tio.core.cluster.core;

import org.tio.core.Node;
import org.tio.core.cluster.message.ClusterDataMessage;
import org.tio.core.cluster.message.ClusterSyncAckMessage;
import org.tio.core.cluster.message.ClusterSyncMessage;

import java.util.Collection;

/**
 * 集群接口
 *
 * @author L.cm
 */
public interface ClusterApi {

	/**
	 * 启动
	 */
	void start() throws Exception;

	/**
	 * 停止
	 */
	void stop();

	/**
	 * 发送消息
	 *
	 * @param member  member
	 * @param message 集群消息
	 * @return 消息id.
	 */
	boolean send(Node member, ClusterDataMessage message);

	/**
	 * 同步发送消息
	 *
	 * @param member  Node
	 * @param message 集群消息
	 * @return 消息id.
	 */
	ClusterSyncAckMessage sendSync(Node member, ClusterSyncMessage message);

	/**
	 * 在集群中广播消息
	 *
	 * @param message 集群消息
	 */
	void broadcast(ClusterDataMessage message);

	/**
	 * 获取集群中的成员，不包含当前成员
	 *
	 * @return 成员列表，不包含自己
	 */
	Collection<Node> getRemoteMembers();

	/**
	 * 获取本地成员
	 *
	 * @return 本地成员
	 */
	Node getLocalMember();

}
