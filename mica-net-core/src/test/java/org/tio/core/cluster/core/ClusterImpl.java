package org.tio.core.cluster.core;

import org.tio.client.TioClientConfig;
import org.tio.core.Node;
import org.tio.core.cluster.message.ClusterDataMessage;
import org.tio.core.cluster.message.ClusterSyncAckMessage;
import org.tio.core.cluster.message.ClusterSyncMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 集群实现
 *
 * @author L.cm
 */
public class ClusterImpl implements ClusterApi {
	private final ClusterConfig config;
	/**
	 * 本地成员
	 */
	private final Node localMember;
	/**
	 * 种子成员
	 */
	private List<Node> seedMembers;
	/**
	 * 后加入的成员
	 */
	private List<Node> lateAddMembers;
	/**
	 * 远程的客户端结合，用于发送消息
	 */
	private ConcurrentMap<Node, TioClientConfig> remoteClientConfigs = new ConcurrentHashMap<>();

	public ClusterImpl(ClusterConfig config) {
		this.config = config;
		this.localMember = null;
	}

	@Override
	public void start() {

	}

	@Override
	public void stop() {

	}

	@Override
	public String send(Node address, ClusterDataMessage message) {
		return null;
	}

	@Override
	public ClusterSyncAckMessage sendSync(Node address, ClusterSyncMessage message) {
		return null;
	}

	@Override
	public String broadcast(ClusterDataMessage message) {
		return null;
	}

	@Override
	public void listen(ClusterMessageListener listener) {

	}

	@Override
	public Collection<Node> getRemoteMembers() {
		return remoteClientConfigs.keySet();
	}

	@Override
	public Node getLocalMember() {
		return this.localMember;
	}
}
