package org.tio.server.cluster.core;

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
	/**
	 * 群组是否集群（同一个群组是否会分布在不同的机器上），false:不集群，默认不集群
	 */
	private boolean cluster4group = false;
	/**
	 * 用户是否集群（同一个用户是否会分布在不同的机器上），false:不集群，默认集群
	 */
	private boolean cluster4user = true;
	/**
	 * ip是否集群（同一个ip是否会分布在不同的机器上），false:不集群，默认集群
	 */
	private boolean cluster4ip = true;
	/**
	 * id是否集群（在A机器上的客户端是否可以通过channelId发消息给B机器上的客户端），false:不集群，默认集群<br>
	 */
	private boolean cluster4channelId = true;
	/**
	 * bsid是否集群（在A机器上的客户端是否可以通过bsid发消息给B机器上的客户端），false:不集群，默认集群<br>
	 */
	private boolean cluster4bsId = true;
	/**
	 * 所有连接是否集群（同一个ip是否会分布在不同的机器上），false:不集群，默认集群
	 */
	private boolean cluster4all = true;

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

	public void addSeedMember(String ip, int port) {
		this.seedMembers.add(new Node(ip, port));
	}

	public void addSeedMember(Node seedMember) {
		this.seedMembers.add(seedMember);
	}

	public void addSeedMembers(List<Node> seedMembers) {
		this.seedMembers.addAll(seedMembers);
	}

	public boolean isCluster4group() {
		return cluster4group;
	}

	public void setCluster4group(boolean cluster4group) {
		this.cluster4group = cluster4group;
	}

	public boolean isCluster4user() {
		return cluster4user;
	}

	public void setCluster4user(boolean cluster4user) {
		this.cluster4user = cluster4user;
	}

	public boolean isCluster4ip() {
		return cluster4ip;
	}

	public void setCluster4ip(boolean cluster4ip) {
		this.cluster4ip = cluster4ip;
	}

	public boolean isCluster4channelId() {
		return cluster4channelId;
	}

	public void setCluster4channelId(boolean cluster4channelId) {
		this.cluster4channelId = cluster4channelId;
	}

	public boolean isCluster4bsId() {
		return cluster4bsId;
	}

	public void setCluster4bsId(boolean cluster4bsId) {
		this.cluster4bsId = cluster4bsId;
	}

	public boolean isCluster4all() {
		return cluster4all;
	}

	public void setCluster4all(boolean cluster4all) {
		this.cluster4all = cluster4all;
	}
}
