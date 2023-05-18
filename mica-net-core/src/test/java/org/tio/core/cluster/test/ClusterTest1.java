package org.tio.core.cluster.test;

import org.tio.core.Node;
import org.tio.core.cluster.core.ClusterApi;
import org.tio.core.cluster.core.ClusterConfig;
import org.tio.core.cluster.core.ClusterImpl;

import java.util.ArrayList;
import java.util.List;

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

		List<Node> seedMembers = new ArrayList<>();
		seedMembers.add(new Node("127.0.0.1", 3001));
		seedMembers.add(new Node("127.0.0.1", 3002));
		seedMembers.add(new Node("127.0.0.1", 3003));
		config.setSeedMembers(seedMembers);

		ClusterApi cluster = new ClusterImpl(config);
		cluster.start();
	}

}
