package org.tio.core.cluster.core;

import org.tio.core.Node;
import org.tio.core.cluster.message.ClusterDataMessage;

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
	void start();

	/**
	 * 停止
	 */
	void stop();

	/**
	 * 发送消息
	 *
	 * @param address address
	 * @param message 集群消息
	 * @return 消息id.
	 */
	String send(Node address, ClusterDataMessage message);

	/**
	 * 发送消息
	 *
	 * @param member  ClusterMember
	 * @param message 集群消息
	 * @return 消息id.
	 */
	String send(ClusterMember member, ClusterDataMessage message);

	/**
	 * 同步发送消息
	 *
	 * @param member  ClusterMember
	 * @param message 集群消息
	 * @return 消息id.
	 */
	ClusterDataMessage sendSync(ClusterMember member, ClusterDataMessage message);

	/**
	 * 在集群中广播消息
	 *
	 * @param message 集群消息
	 * @return 消息id.
	 */
	String broadcast(ClusterDataMessage message);

	/**
	 * 集群消息监听
	 */
	void listen(ClusterMessageListener listener);

	/**
	 * 获取集群中的成员，包含当前成员
	 *
	 * @return 成员列表
	 */
	Collection<ClusterMember> getMembers();

	/**
	 * 获取集群中的成员，不包含当前成员
	 *
	 * @return 成员列表，不包含自己
	 */
	Collection<ClusterMember> getOtherMembers();

	/**
	 * 获取本地成员
	 *
	 * @return 本地成员
	 */
	ClusterMember getMember();

	/**
	 * 根据成员 id 获取成员
	 *
	 * @return 成员
	 */
	ClusterMember getMember(String id);

	/**
	 * 更加地址获取成员
	 *
	 * @return 成员
	 */
	ClusterMember member(Node address);

}
