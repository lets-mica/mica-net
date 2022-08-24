package org.tio.core.tcp;

import org.tio.client.*;
import org.tio.client.intf.TioClientHandler;
import org.tio.core.Node;
import org.tio.core.Tio;
import org.tio.core.intf.EncodedPacket;

import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public class TcpClientTest {

	public static void main(String[] args) throws Exception {
		TioClientHandler tioHandler = new TestTioClientHandler();
		TioClientConfig config = new TioClientConfig(tioHandler, new DefaultTioClientListener());
		config.setReconnConf(new ReconnConf());
		TioClient tioClient = new TioClient(config);
		ClientChannelContext connect = tioClient.connect(new Node("127.0.0.1", 502));

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				String message = "mica:" + System.currentTimeMillis();
				Tio.send(connect, new EncodedPacket(message.getBytes(StandardCharsets.UTF_8)));
			}
		}, 3000, 3000);

	}

}
