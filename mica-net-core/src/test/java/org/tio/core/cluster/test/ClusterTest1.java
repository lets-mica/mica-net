package org.tio.core.cluster.test;

import org.tio.core.Node;
import org.tio.core.cluster.core.ClusterApi;
import org.tio.core.cluster.core.ClusterConfig;
import org.tio.core.cluster.core.ClusterImpl;

/**
 * 集群开发测试
 *
 * @author L.cm
 */
public class ClusterTest1 {

	public static void main(String[] args) throws Exception {
		ClusterConfig config = new ClusterConfig();
		config.setHost("127.0.0.1");
		config.setPort(3001);

		config.addSeedMember(new Node("127.0.0.1", 3001));
		config.addSeedMember(new Node("127.0.0.1", 3002));
		config.addSeedMember(new Node("127.0.0.1", 3003));

		ClusterApi cluster = new ClusterImpl(config);
		cluster.start();
	}

}
