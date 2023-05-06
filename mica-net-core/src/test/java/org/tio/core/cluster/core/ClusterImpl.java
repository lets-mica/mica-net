package org.tio.core.cluster.core;

import org.tio.core.Node;
import org.tio.core.cluster.message.ClusterDataMessage;

import java.util.Collection;

/**
 * 集群实现
 *
 * @author L.cm
 */
public class ClusterImpl implements ClusterApi {
	private final ClusterConfig config;

	public ClusterImpl(ClusterConfig config) {
		this.config = config;
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
	public ClusterDataMessage sendSync(ClusterMember member, ClusterDataMessage message) {
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
	public Collection<ClusterMember> getOtherMembers() {
		return null;
	}

	@Override
	public ClusterMember getMember() {
		return null;
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
