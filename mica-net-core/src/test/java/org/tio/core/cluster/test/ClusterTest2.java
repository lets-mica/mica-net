package org.tio.core.cluster.test;

import org.tio.core.Node;
import org.tio.core.cluster.core.ClusterApi;
import org.tio.core.cluster.core.ClusterConfig;
import org.tio.core.cluster.core.ClusterImpl;
import org.tio.core.cluster.message.ClusterDataMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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

		ClusterApi cluster = new ClusterImpl(config);
		cluster.start();

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				String message = "hello mica form cluster:" + cluster.getLocalMember();
				cluster.broadcast(new ClusterDataMessage(message.getBytes(StandardCharsets.UTF_8)));
			}
		}, 3000, 3000);
	}

}
