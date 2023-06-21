package org.tio.server.cluster.core;

import org.tio.server.cluster.codec.ClusterMessageDecoder;
import org.tio.server.cluster.message.ClusterDataMessage;
import org.tio.server.cluster.message.ClusterSyncAckMessage;
import org.tio.server.cluster.message.ClusterSyncMessage;
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
import org.tio.utils.Threads;
import org.tio.utils.hutool.Snowflake;
import org.tio.utils.thread.pool.SynThreadPoolExecutor;
import org.tio.utils.timer.TimerTask;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

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
		this.seedMembers = filterSeedMembers(config, this.localMember);
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
	private static List<Node> filterSeedMembers(ClusterConfig config, Node localMember) {
		List<Node> seedMembers = config.getSeedMembers();
		seedMembers.remove(localMember);
		return Collections.unmodifiableList(seedMembers);
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
		int tioPoolSize = Threads.AVAILABLE_PROCESSORS + 1;
		// 线程配置
		SynThreadPoolExecutor tioExecutor = Threads.getTioExecutor(tioPoolSize);
		ThreadPoolExecutor groupExecutor = Threads.getGroupExecutor(tioPoolSize * 2);
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
		clientConfig.setReconnConf(new ReconnConf());
		clientConfig.setTioUuid(new SnowflakeTioUuid());
		this.tcpClusterClient = new TioClient(clientConfig);
		for (Node seedMember : seedMembers) {
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
		TioClientConfig clientConfig = this.tcpClusterClient.getTioClientConfig();
		ChannelContext context = Tio.getByClientNode(clientConfig, address);
		return Tio.send(context, new ClusterDataMessage(data));
	}

	@Override
	public ClusterSyncAckMessage sendSync(Node address, byte[] message) {
		// context
		TioClientConfig clientConfig = this.tcpClusterClient.getTioClientConfig();
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
		TioClientConfig clientConfig = this.tcpClusterClient.getTioClientConfig();
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
	protected void addJoinMember(Node joinMember) {
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
