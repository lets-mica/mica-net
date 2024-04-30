package org.tio.core.stat.vo;

/**
 * stat 统计 vo
 */
public class StatVo {

	/**
	 * 连接信息
	 */
	private ConnectStatVo connections;
	/**
	 * 消息信息
	 */
	private MessageStatVo messages;
	/**
	 * 节点统计
	 */
	private NodeStatVo nodes;

	public ConnectStatVo getConnections() {
		return connections;
	}

	public void setConnections(ConnectStatVo connections) {
		this.connections = connections;
	}

	public MessageStatVo getMessages() {
		return messages;
	}

	public void setMessages(MessageStatVo messages) {
		this.messages = messages;
	}

	public NodeStatVo getNodes() {
		return nodes;
	}

	public void setNodes(NodeStatVo nodes) {
		this.nodes = nodes;
	}

	@Override
	public String toString() {
		return "StatVo{" +
			"connections=" + connections +
			", messages=" + messages +
			", nodes=" + nodes +
			'}';
	}
}
