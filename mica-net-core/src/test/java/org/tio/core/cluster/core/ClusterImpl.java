package org.tio.core.cluster.core;

import org.tio.client.ReconnConf;
import org.tio.client.TioClient;
import org.tio.client.TioClientConfig;
import org.tio.client.intf.TioClientHandler;
import org.tio.client.intf.TioClientListener;
import org.tio.core.ChannelContext;
import org.tio.core.Node;
import org.tio.core.Tio;
import org.tio.core.cluster.codec.ClusterMessageDecoder;
import org.tio.core.cluster.message.ClusterDataMessage;
import org.tio.core.cluster.message.ClusterSyncAckMessage;
import org.tio.core.cluster.message.ClusterSyncMessage;
import org.tio.core.cluster.transport.ClusterTcpClientHandler;
import org.tio.core.cluster.transport.ClusterTcpClientListener;
import org.tio.core.cluster.transport.ClusterTcpServerHandler;
import org.tio.server.DefaultTioServerListener;
import org.tio.server.TioServer;
import org.tio.server.TioServerConfig;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 集群实现
 *
 * @author L.cm
 */
public class ClusterImpl implements ClusterApi {
	private final ClusterConfig config;
	/**
	 * 本地成员
	 */
	private final Node localMember;
	/**
	 * 种子成员
	 */
	private List<Node> seedMembers;
	/**
	 * 后加入的成员
	 */
	private List<Node> lateAddMembers;
	/**
	 * 远程的客户端结合，用于发送消息
	 */
	private ConcurrentMap<Node, TioClientConfig> remoteClientConfigs = new ConcurrentHashMap<>();
	/**
	 * tcp 集群服务
	 */
	private TioServer tcpClusterServer;
	/**
	 * tcp 客户端
	 */
	private TioClient tcpClusterClient;
	private final ClusterMessageDecoder messageDecoder;

	public ClusterImpl(ClusterConfig config) {
		this.config = config;
		this.localMember = new Node(config.getHost(), config.getPort());
		this.messageDecoder = new ClusterMessageDecoder();
	}

	@Override
	public void start() throws Exception {
		// 1. 先启动服务
		startClusterTcpService();
		// 2. 启动客户端
		startClusterTcpClient();
	}

	private void startClusterTcpService() throws IOException {
		// 配置
		TioServerConfig serverConfig = new TioServerConfig(new ClusterTcpServerHandler(messageDecoder), new DefaultTioServerListener());
		serverConfig.setName("TCP-cluster-server");
		// 高位在前
		serverConfig.setReadBufferSize(1024 * 8);
		// 心跳改为 1 小时
		this.tcpClusterServer = new TioServer(serverConfig);
		this.tcpClusterServer.start("0.0.0.0", config.getPort());
	}

	private void startClusterTcpClient() throws Exception {
		TioClientHandler tioHandler = new ClusterTcpClientHandler(this.messageDecoder);
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
		return null;
	}

	@Override
	public void broadcast(ClusterDataMessage message) {
		TioClientConfig clientConfig = this.tcpClusterClient.getTioClientConfig();
		Set<ChannelContext> contextSet = Tio.getConnecteds(clientConfig);
		Tio.sendToSet(clientConfig, contextSet, message, null);
	}

	@Override
	public void listen(ClusterMessageListener listener) {

	}

	@Override
	public Collection<Node> getRemoteMembers() {
		return remoteClientConfigs.keySet();
	}

	@Override
	public Node getLocalMember() {
		return this.localMember;
	}
}
