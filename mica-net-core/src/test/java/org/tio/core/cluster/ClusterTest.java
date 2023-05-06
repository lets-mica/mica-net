package org.tio.core.cluster;

import org.tio.core.cluster.transport.ClusterTcpServerHandler;
import org.tio.server.DefaultTioServerListener;
import org.tio.server.TioServer;
import org.tio.server.TioServerConfig;

import java.io.IOException;

/**
 * 集群开发测试
 *
 * @author L.cm
 */
public class ClusterTest {



	public static TioServer clusterTcpService(int port) throws IOException {
		// 配置
		TioServerConfig config = new TioServerConfig(new ClusterTcpServerHandler(), new DefaultTioServerListener());
		config.setName("TCP-cluster-server");
		// 高位在前
		config.setReadBufferSize(1024 * 8);
		// 心跳改为 1 小时
		TioServer tioServer = new TioServer(config);
		tioServer.start("0.0.0.0", port);
		return tioServer;
	}

}
