package org.tio.core.cluster.test;

import org.tio.client.ReconnConf;
import org.tio.client.TioClient;
import org.tio.client.TioClientConfig;
import org.tio.client.intf.TioClientHandler;
import org.tio.client.intf.TioClientListener;
import org.tio.core.Node;
import org.tio.core.cluster.codec.ClusterMessageDecoder;
import org.tio.core.cluster.transport.ClusterTcpClientHandler;
import org.tio.core.cluster.transport.ClusterTcpClientListener;
import org.tio.core.cluster.transport.ClusterTcpServerHandler;
import org.tio.server.DefaultTioServerListener;
import org.tio.server.TioServer;
import org.tio.server.TioServerConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * 集群开发测试
 *
 * @author L.cm
 */
public class ClusterTest3 {

	public static void main(String[] args) throws Exception {
		ClusterMessageDecoder messageDecoder = new ClusterMessageDecoder();
		int port = 3003;
		TioServer tioServer = getClusterTcpService(port, messageDecoder);
		List<Node> seedMembers = new ArrayList<>();
		seedMembers.add(new Node("127.0.0.1", 3001));
		seedMembers.add(new Node("127.0.0.1", 3002));
		seedMembers.add(new Node("127.0.0.1", 3003));
		TioClientHandler tioHandler = new ClusterTcpClientHandler(messageDecoder);
		TioClientListener tioListener = new ClusterTcpClientListener();
		List<TioClient> clientList = getClusterTcpClientList(seedMembers, tioHandler, tioListener);
	}

	public static void test1() throws UnknownHostException {
		InetAddress address1 = InetAddress.getByName("127.0.0.1");
		InetAddress address2 = InetAddress.getByName("192.168.210.129");
		boolean siteLocalAddress = address2.isSiteLocalAddress();
		System.out.println(address2);
	}

	public static TioServer getClusterTcpService(int port, ClusterMessageDecoder messageDecoder) throws IOException {
		// 配置
		TioServerConfig config = new TioServerConfig(new ClusterTcpServerHandler(messageDecoder), new DefaultTioServerListener());
		config.setName("TCP-cluster-server");
		// 高位在前
		config.setReadBufferSize(1024 * 8);
		// 心跳改为 1 小时
		TioServer tioServer = new TioServer(config);
		tioServer.start("0.0.0.0", port);
		return tioServer;
	}

	public static List<TioClient> getClusterTcpClientList(
		List<Node> seedMembers, TioClientHandler tioHandler, TioClientListener tioListener
	) throws Exception {
		List<TioClient> clientList = new ArrayList<>();
		for (Node seedMember : seedMembers) {
			clientList.add(getClusterTcpClient(seedMember, tioHandler, tioListener));
		}
		return clientList;
	}

	public static TioClient getClusterTcpClient(
		Node seedMember, TioClientHandler tioHandler, TioClientListener tioListener
	) throws Exception {
		// 配置
		TioClientConfig config = new TioClientConfig(tioHandler, tioListener);
		config.setReconnConf(new ReconnConf());
		TioClient tioClient = new TioClient(config);
		tioClient.connect(seedMember);
		return tioClient;
	}

}
