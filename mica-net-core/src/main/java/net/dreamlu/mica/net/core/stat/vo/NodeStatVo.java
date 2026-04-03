package net.dreamlu.mica.net.core.stat.vo;

/**
 * 节点统计
 *
 * @author L.cm
 */
public class NodeStatVo {

	/**
	 * 客户端节点
	 */
	private long clientNodes;
	/**
	 * 连接数
	 */
	private long connections;
	/**
	 * 用户数
	 */
	private long users;

	public long getClientNodes() {
		return clientNodes;
	}

	public void setClientNodes(long clientNodes) {
		this.clientNodes = clientNodes;
	}

	public long getConnections() {
		return connections;
	}

	public void setConnections(long connections) {
		this.connections = connections;
	}

	public long getUsers() {
		return users;
	}

	public void setUsers(long users) {
		this.users = users;
	}

	@Override
	public String toString() {
		return "NodeStatVo{" +
			"clientNodes=" + clientNodes +
			", connections=" + connections +
			", users=" + users +
			'}';
	}
}
