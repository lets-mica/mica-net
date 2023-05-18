package org.tio.core.cluster.core;

import org.tio.core.Node;

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
	private String host;
	/**
	 * 集群端口
	 */
	private int port;
	/**
	 * 种子成员
	 */
	private List<Node> seedMembers;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public List<Node> getSeedMembers() {
		return seedMembers;
	}

	public void setSeedMembers(List<Node> seedMembers) {
		this.seedMembers = seedMembers;
	}

}
