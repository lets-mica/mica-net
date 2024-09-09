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

package org.tio.server.cluster.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.client.ReconnConf;
import org.tio.client.TioClient;
import org.tio.client.TioClientConfig;
import org.tio.client.intf.TioClientHandler;
import org.tio.client.intf.TioClientListener;
import org.tio.core.ChannelContext;
import org.tio.core.Node;
import org.tio.core.Tio;
import org.tio.core.uuid.SnowflakeTioUuid;
import org.tio.server.TioServer;
import org.tio.server.TioServerConfig;
import org.tio.server.cluster.codec.ClusterMessageDecoder;
import org.tio.server.cluster.message.ClusterDataMessage;
import org.tio.server.cluster.message.ClusterSyncAckMessage;
import org.tio.server.cluster.message.ClusterSyncMessage;
import org.tio.utils.hutool.Snowflake;
import org.tio.utils.thread.ThreadUtils;
import org.tio.utils.thread.pool.SynThreadPoolExecutor;
import org.tio.utils.timer.TimerTask;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 集群实现
 *
 * @author L.cm
 */
public class ClusterImpl implements ClusterApi {
	private static final Logger log = LoggerFactory.getLogger(ClusterImpl.class);
	/**
	 * 集群配置
	 */
	private final ClusterConfig config;
	/**
	 * 本地成员
	 */
	private final Node localMember;
	/**
	 * 种子成员
	 */
	private final List<Node> seedMembers;
	/**
	 * 后加入的成员
	 */
	private final List<Node> lateJoinMembers;
	/**
	 * tcp 集群服务
	 */
	private TioServer tcpClusterServer;
	/**
	 * tcp 客户端
	 */
	private TioClient tcpClusterClient;
	private final ClusterMessageDecoder messageDecoder;
	/**
	 * 同步消息处理，key：messageId，value：CompletableFuture
	 */
	private final ConcurrentMap<Long, CompletableFuture<ClusterSyncAckMessage>> syncMessageMap;
	/**
	 * id 生产器
	 */
	private final Snowflake snowflake;

	public ClusterImpl(ClusterConfig config) {
		this.config = config;
		this.localMember = new Node(config.getHost(), config.getPort());
		this.seedMembers = filterSeedMembers(config);
		this.lateJoinMembers = new ArrayList<>();
		this.messageDecoder = new ClusterMessageDecoder();
		this.syncMessageMap = new ConcurrentHashMap<>();
		this.snowflake = new Snowflake(ThreadLocalRandom.current().nextInt(1, 30), ThreadLocalRandom.current().nextInt(1, 30));
	}

	/**
	 * 过滤种子成员，去掉自己
	 *
	 * @return members
	 */
	private static List<Node> filterSeedMembers(ClusterConfig config) {
		return config.getSeedMembers().stream()
			.distinct()
			.collect(Collectors.toList());
	}

	@Override
	public void start() throws Exception {
		// 1. 先启动服务
		startClusterTcpService();
		// 2. 启动客户端
		startClusterTcpClient();
	}

	private void startClusterTcpService() throws IOException {
		ClusterMessageListener messageListener = this.config.getMessageListener();
		ClusterTcpServerHandler serverHandler = new ClusterTcpServerHandler(this, messageDecoder, messageListener);
		// 配置
		String name = "TCP-cluster-server";
		int tioPoolSize = ThreadUtils.AVAILABLE_PROCESSORS + 1;
		// 线程配置
		SynThreadPoolExecutor tioExecutor = ThreadUtils.getTioExecutor(tioPoolSize);
		ExecutorService groupExecutor = ThreadUtils.getGroupExecutor(tioPoolSize * 2);
		TioServerConfig serverConfig = new TioServerConfig(
			name, serverHandler, new ClusterTcpServerListener(), tioExecutor, groupExecutor
		);
		this.tcpClusterServer = new TioServer(serverConfig);
		this.tcpClusterServer.start("0.0.0.0", config.getPort());
	}

	private void startClusterTcpClient() throws Exception {
		TioClientHandler tioHandler = new ClusterTcpClientHandler(this.messageDecoder, syncMessageMap);
		TioClientListener tioListener = new ClusterTcpClientListener(this);
		// 配置
		TioClientConfig clientConfig = new TioClientConfig(tioHandler, tioListener);
		clientConfig.setName("TCP-cluster-client");
		// 1s 重连
		clientConfig.setReconnConf(new ReconnConf(1000));
		clientConfig.setTioUuid(new SnowflakeTioUuid());
		this.tcpClusterClient = new TioClient(clientConfig);
		// 移除本地节点
		List<Node> clientNodes = new ArrayList<>(seedMembers);
		clientNodes.remove(localMember);
		for (Node seedMember : clientNodes) {
			this.tcpClusterClient.connect(seedMember);
		}
	}

	@Override
	public void stop() {
		// 1. 停止客户端
		this.tcpClusterClient.stop();
		// 2. 停止服务端
		this.tcpClusterServer.stop();
	}

	@Override
	public boolean send(Node address, byte[] data) {
		TioClientConfig clientConfig = this.tcpClusterClient.getClientConfig();
		ChannelContext context = Tio.getByClientNode(clientConfig, address);
		return Tio.send(context, new ClusterDataMessage(data));
	}

	@Override
	public ClusterSyncAckMessage sendSync(Node address, byte[] message) {
		// context
		TioClientConfig clientConfig = this.tcpClusterClient.getClientConfig();
		ChannelContext context = Tio.getByClientNode(clientConfig, address);
		// messageId
		long messageId = this.snowflake.nextId();
		CompletableFuture<ClusterSyncAckMessage> future = new CompletableFuture<>();
		syncMessageMap.put(messageId, future);
		// 发送消息
		Tio.send(context, new ClusterSyncMessage(messageId, message));
		// 等待回调
		return future.join();
	}

	@Override
	public void broadcast(byte[] data) {
		TioClientConfig clientConfig = this.tcpClusterClient.getClientConfig();
		Set<ChannelContext> contextSet = Tio.getConnecteds(clientConfig);
		Tio.sendToSet(clientConfig, contextSet, new ClusterDataMessage(data), null);
	}

	@Override
	public TimerTask schedule(Runnable command, long delay) {
		return this.tcpClusterClient.schedule(command, delay);
	}

	@Override
	public TimerTask schedule(Runnable command, long delay, Executor executor) {
		return this.tcpClusterClient.schedule(command, delay, executor);
	}

	@Override
	public TimerTask scheduleOnce(Runnable command, long delay) {
		return this.tcpClusterClient.scheduleOnce(command, delay);
	}

	@Override
	public TimerTask scheduleOnce(Runnable command, long delay, Executor executor) {
		return this.tcpClusterClient.scheduleOnce(command, delay, executor);
	}

	@Override
	public boolean isLateJoinMember() {
		return !this.config.getSeedMembers().contains(this.localMember);
	}

	@Override
	public Collection<Node> getSeedMembers() {
		return Collections.unmodifiableList(this.config.getSeedMembers());
	}

	@Override
	public Collection<Node> getRemoteMembers() {
		Set<Node> remoteMembers = new HashSet<>(seedMembers);
		remoteMembers.addAll(lateJoinMembers);
		remoteMembers.remove(localMember);
		return Collections.unmodifiableSet(remoteMembers);
	}

	@Override
	public Node getLocalMember() {
		return this.localMember;
	}

	/**
	 * 后加入进来的节点
	 *
	 * @param joinMember joinMember
	 */
	protected synchronized void addJoinMember(Node joinMember) {
		// 新加入的节点
		if (!lateJoinMembers.contains(joinMember)) {
			this.lateJoinMembers.add(joinMember);
			try {
				this.tcpClusterClient.connect(joinMember);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}
}
