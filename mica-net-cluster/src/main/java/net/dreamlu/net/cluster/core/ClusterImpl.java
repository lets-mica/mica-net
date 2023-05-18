package net.dreamlu.net.cluster.core;

import net.dreamlu.net.cluster.codec.ClusterMessageDecoder;
import net.dreamlu.net.cluster.message.ClusterDataMessage;
import net.dreamlu.net.cluster.message.ClusterSyncAckMessage;
import net.dreamlu.net.cluster.message.ClusterSyncMessage;
import net.dreamlu.net.cluster.transport.ClusterTcpClientHandler;
import net.dreamlu.net.cluster.transport.ClusterTcpClientListener;
import net.dreamlu.net.cluster.transport.ClusterTcpServerHandler;
import net.dreamlu.net.cluster.transport.ClusterTcpServerListener;
import org.tio.client.ReconnConf;
import org.tio.client.TioClient;
import org.tio.client.TioClientConfig;
import org.tio.client.intf.TioClientHandler;
import org.tio.client.intf.TioClientListener;
import org.tio.core.ChannelContext;
import org.tio.core.Node;
import org.tio.core.Tio;
import org.tio.server.TioServer;
import org.tio.server.TioServerConfig;
import org.tio.utils.timer.TimerTask;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

/**
 * 集群实现
 *
 * @author L.cm
 */
public class ClusterImpl implements ClusterApi {
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
	private final ConcurrentMap<String, CompletableFuture<ClusterSyncAckMessage>> syncMessageMap;

	public ClusterImpl(ClusterConfig config) {
		this.config = config;
		this.localMember = new Node(config.getHost(), config.getPort());
		this.seedMembers = filterSeedMembers(config, this.localMember);
		this.lateJoinMembers = new ArrayList<>();
		this.messageDecoder = new ClusterMessageDecoder();
		this.syncMessageMap = new ConcurrentHashMap<>();
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
		ClusterTcpServerHandler serverHandler = new ClusterTcpServerHandler(messageDecoder, messageListener);
		// 配置
		TioServerConfig serverConfig = new TioServerConfig(serverHandler, new ClusterTcpServerListener());
		serverConfig.setName("TCP-cluster-server");
		// 高位在前
		serverConfig.setReadBufferSize(1024 * 8);
		// 心跳改为 1 小时
		this.tcpClusterServer = new TioServer(serverConfig);
		this.tcpClusterServer.start("0.0.0.0", config.getPort());
	}

	private void startClusterTcpClient() throws Exception {
		TioClientHandler tioHandler = new ClusterTcpClientHandler(this.messageDecoder, syncMessageMap);
		TioClientListener tioListener = new ClusterTcpClientListener();
		// 配置
		TioClientConfig clientConfig = new TioClientConfig(tioHandler, tioListener);
		clientConfig.setReconnConf(new ReconnConf());
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
	public boolean send(Node address, ClusterDataMessage message) {
		TioClientConfig clientConfig = this.tcpClusterClient.getTioClientConfig();
		ChannelContext context = Tio.getByClientNode(clientConfig, address);
		return Tio.send(context, message);
	}

	@Override
	public ClusterSyncAckMessage sendSync(Node address, ClusterSyncMessage message) {
		String messageId = message.getMessageId();
		CompletableFuture<ClusterSyncAckMessage> future = new CompletableFuture<>();
		syncMessageMap.put(messageId, future);
		// 等待回调
		return future.join();
	}

	@Override
	public void broadcast(ClusterDataMessage message) {
		TioClientConfig clientConfig = this.tcpClusterClient.getTioClientConfig();
		Set<ChannelContext> contextSet = Tio.getConnecteds(clientConfig);
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

}
