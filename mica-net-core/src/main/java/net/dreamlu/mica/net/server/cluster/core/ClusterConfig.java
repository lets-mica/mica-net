/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & www.dreamlu.net).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dreamlu.mica.net.server.cluster.core;

import net.dreamlu.mica.net.core.Node;

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
	 * server 侧消息监听器（处理来自其它节点主动发来的 ClusterDataMessage）
	 */
	private final ClusterMessageListener messageListener;
	/**
	 * client 侧消息监听器（处理本节点作为 client 时收到的 ClusterDataMessage），可为 null
	 */
	private final ClusterMessageListener clientMessageListener;
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
		this(host, port, messageListener, null);
	}

	/**
	 * 构造集群配置，同时注册 server 侧与 client 侧的 ClusterDataMessage 回调。
	 *
	 * @param host                  集群间互相可访问的 ip 或者域名
	 * @param port                  集群端口
	 * @param messageListener       server 侧消息监听器（处理其它节点主动发来的 ClusterDataMessage）
	 * @param clientMessageListener client 侧消息监听器（处理本节点作为 client 时收到的 ClusterDataMessage），可为 null
	 */
	public ClusterConfig(String host, int port, ClusterMessageListener messageListener,
						 ClusterMessageListener clientMessageListener) {
		this.host = host;
		this.port = port;
		this.messageListener = messageListener;
		this.clientMessageListener = clientMessageListener;
	}

	public ClusterMessageListener getMessageListener() {
		return messageListener;
	}

	public ClusterMessageListener getClientMessageListener() {
		return clientMessageListener;
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
