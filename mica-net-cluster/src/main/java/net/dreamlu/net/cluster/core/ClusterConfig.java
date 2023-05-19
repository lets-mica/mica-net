package net.dreamlu.net.cluster.core;

import org.tio.core.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * 集群配置
 *
 * @author L.cm
 */
public class ClusterConfig {

	/**
	 * 集群间互相可访问的 ip 或者域名
	 */
	private final String host;
	/**
	 * 集群端口
	 */
	private final int port;
	/**
	 * 种子成员
	 */
	private final List<Node> seedMembers = new ArrayList<>();
	/**
	 * 消息监听器
	 */
	private final ClusterMessageListener messageListener;

	public ClusterConfig(String host, int port, ClusterMessageListener messageListener) {
		this.host = host;
		this.port = port;
		this.messageListener = messageListener;
	}

	public ClusterMessageListener getMessageListener() {
		return messageListener;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public List<Node> getSeedMembers() {
		return seedMembers;
	}

	public void addSeedMember(Node seedMember) {
		this.seedMembers.add(seedMember);
	}

	public void addSeedMembers(List<Node> seedMembers) {
		this.seedMembers.addAll(seedMembers);
	}

}
