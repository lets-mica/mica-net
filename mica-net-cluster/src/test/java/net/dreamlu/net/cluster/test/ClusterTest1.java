package net.dreamlu.net.cluster.test;

import net.dreamlu.net.cluster.message.ClusterDataMessage;
import org.tio.core.Node;
import net.dreamlu.net.cluster.core.ClusterApi;
import net.dreamlu.net.cluster.core.ClusterConfig;
import net.dreamlu.net.cluster.core.ClusterImpl;

import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

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

		config.setMessageListener(message -> {
			System.out.println(new String(message.getPayload()));
		});

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
