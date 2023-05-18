package org.tio.core.cluster.core;

import org.tio.core.Node;

import java.util.List;
import java.util.Objects;

/**
 * 集群配置
 *
 * @author L.cm
 */
public class ClusterConfig {

	/**
	 * 集群端口
	 */
	private int port;
	/**
	 * 种子成员
	 */
	private List<Node> seedMembers;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ClusterConfig that = (ClusterConfig) o;
		return port == that.port && Objects.equals(seedMembers, that.seedMembers);
	}

	@Override
	public int hashCode() {
		return Objects.hash(port, seedMembers);
	}

	@Override
	public String toString() {
		return "ClusterConfig{" +
			"port=" + port +
			", seedMembers=" + seedMembers +
			'}';
	}
}
