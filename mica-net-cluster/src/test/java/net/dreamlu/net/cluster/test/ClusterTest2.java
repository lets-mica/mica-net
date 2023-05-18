package net.dreamlu.net.cluster.test;

import net.dreamlu.net.cluster.core.ClusterApi;
import net.dreamlu.net.cluster.core.ClusterConfig;
import net.dreamlu.net.cluster.core.ClusterImpl;
import net.dreamlu.net.cluster.message.ClusterDataMessage;
import org.tio.core.Node;

import java.nio.charset.StandardCharsets;

/**
 * 集群开发测试
 *
 * @author L.cm
 */
public class ClusterTest2 {

	public static void main(String[] args) throws Exception {
		ClusterConfig config = new ClusterConfig();
		config.setHost("127.0.0.1");
		config.setPort(3002);

		config.addSeedMember(new Node("127.0.0.1", 3001));
		config.addSeedMember(new Node("127.0.0.1", 3002));
		config.addSeedMember(new Node("127.0.0.1", 3003));

		config.setMessageListener(message -> {
			System.out.println(new String(message.getPayload()));
		});

		ClusterApi cluster = new ClusterImpl(config);
		cluster.start();

		cluster.schedule(() -> {
			String message = "hello mica form cluster:" + cluster.getLocalMember();
			cluster.broadcast(new ClusterDataMessage(message.getBytes(StandardCharsets.UTF_8)));
		}, 3000);
	}

}
