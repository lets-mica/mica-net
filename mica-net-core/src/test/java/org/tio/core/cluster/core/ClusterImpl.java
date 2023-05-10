package org.tio.core.cluster.core;

import org.tio.core.Node;
import org.tio.core.cluster.message.ClusterDataMessage;
import org.tio.core.cluster.message.ClusterSyncAckMessage;
import org.tio.core.cluster.message.ClusterSyncMessage;

import java.util.*;

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
	private final ClusterMember localMember;
	/**
	 * 所有成员
	 */
	private final List<ClusterMember> remoteMembers = new ArrayList<>();

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
	public String send(ClusterMember member, ClusterDataMessage message) {
		return null;
	}

	@Override
	public ClusterSyncAckMessage sendSync(ClusterMember member, ClusterSyncMessage message) {
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
	public Collection<ClusterMember> getMembers() {
		return null;
	}

	@Override
	public Collection<ClusterMember> getRemoteMembers() {
		return this.remoteMembers;
	}

	@Override
	public ClusterMember getMember() {
		return this.localMember;
	}

	@Override
	public ClusterMember getMember(String id) {
		return null;
	}

	@Override
	public ClusterMember member(Node address) {
		return null;
	}
}
