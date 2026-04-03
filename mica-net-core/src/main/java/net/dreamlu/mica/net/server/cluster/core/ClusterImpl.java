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

import net.dreamlu.mica.net.client.ReconnConf;
import net.dreamlu.mica.net.client.TioClient;
import net.dreamlu.mica.net.client.TioClientConfig;
import net.dreamlu.mica.net.client.intf.TioClientHandler;
import net.dreamlu.mica.net.client.intf.TioClientListener;
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.Node;
import net.dreamlu.mica.net.core.Tio;
import net.dreamlu.mica.net.core.uuid.SnowflakeTioUuid;
import net.dreamlu.mica.net.server.TioServer;
import net.dreamlu.mica.net.server.TioServerConfig;
import net.dreamlu.mica.net.server.cluster.codec.ClusterMessageDecoder;
import net.dreamlu.mica.net.server.cluster.message.ClusterDataMessage;
import net.dreamlu.mica.net.server.cluster.message.ClusterSyncAckMessage;
import net.dreamlu.mica.net.server.cluster.message.ClusterSyncMessage;
import net.dreamlu.mica.net.utils.hutool.Snowflake;
import net.dreamlu.mica.net.utils.thread.ThreadUtils;
import net.dreamlu.mica.net.utils.thread.pool.SynThreadPoolExecutor;
import net.dreamlu.mica.net.utils.timer.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private final Set<Node> lateJoinMembers;
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
	 * 集群成员节点与 ChannelContext 的映射
	 * key: 连接时使用的 Node（connect 参数 / serverNode），与配置一致，避免 IPv4/IPv6 格式差异导致查找失败
	 */
	private final ConcurrentMap<Node, ChannelContext> memberChannels = new ConcurrentHashMap<>();
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
		this.lateJoinMembers = ConcurrentHashMap.newKeySet();
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
		this.tcpClusterServer = new TioServer(config.getPort(), serverConfig);
		this.tcpClusterServer.start();
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
		// 3. 清理映射
		this.memberChannels.clear();
	}

	@Override
	public boolean send(Node member, ClusterDataMessage message) {
		ChannelContext context = memberChannels.get(member);
		if (context == null) {
			log.warn("节点:{} 可能不在线", member);
			return false;
		}
		return Tio.send(context, message);
	}

	@Override
	public ClusterSyncAckMessage sendSync(Node member, ClusterDataMessage message) {
		ChannelContext context = memberChannels.get(member);
		if (context == null) {
			log.warn("节点:{} 可能不在线", member);
			return null;
		}
		long messageId = this.snowflake.nextId();
		CompletableFuture<ClusterSyncAckMessage> future = new CompletableFuture<>();
		syncMessageMap.put(messageId, future);
		try {
			Tio.send(context, new ClusterSyncMessage(messageId, message));
			return future.get(10, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			log.warn("同步消息超时, messageId:{}, member:{}", messageId, member);
			future.cancel(false);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error(e.getMessage(), e);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			syncMessageMap.remove(messageId);
		}
		return null;
	}

	@Override
	public void broadcast(ClusterDataMessage message) {
		// 所有在线节点
		Set<ChannelContext> contextSet = new HashSet<>(memberChannels.values());
		TioClientConfig clientConfig = this.tcpClusterClient.getClientConfig();
		Tio.sendToSet(clientConfig, contextSet, message, null);
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
	 * 连接建立时注册：Node → ChannelContext
	 *
	 * @param node    连接目标节点（serverNode，与 connect 参数一致）
	 * @param context 对应的 ChannelContext
	 */
	void putMemberChannel(Node node, ChannelContext context) {
		memberChannels.put(node, context);
	}

	/**
	 * 连接关闭时移除：Node → ChannelContext
	 * 使用 remove(key, value) 精确匹配，避免重连场景下误删新连接
	 *
	 * @param node    连接目标节点
	 * @param context 对应的 ChannelContext
	 */
	void removeMemberChannel(Node node, ChannelContext context) {
		memberChannels.remove(node, context);
	}

	/**
	 * 收到新节点 JOIN 消息后，将其加入集群并建立返回连接。
	 * <p>
	 * 拓扑设计：星形拓扑下，新节点主动连种子节点，种子节点收到 JOIN 后主动连回新节点，
	 * 保证每对节点间都是双向 TCP 连接。
	 *
	 * @param joinMember 新加入的节点
	 */
	void addJoinMember(Node joinMember) {
		if (lateJoinMembers.add(joinMember)) {
			try {
				// 主动建立到新节点的 client 连接（返回连接）
				this.tcpClusterClient.connect(joinMember);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}
}
