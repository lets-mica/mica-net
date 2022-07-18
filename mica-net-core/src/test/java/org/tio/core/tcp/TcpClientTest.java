package org.tio.core.tcp;

import org.tio.client.DefaultTioClientListener;
import org.tio.client.TioClient;
import org.tio.client.TioClientConfig;
import org.tio.client.intf.TioClientHandler;
import org.tio.core.Node;

public class TcpClientTest {

	public static void main(String[] args) throws Exception {
		TioClientHandler tioHandler = new TestTioClientHandler();
		TioClientConfig config = new TioClientConfig(tioHandler, new DefaultTioClientListener());
		TioClient tioClient = new TioClient(config);
		tioClient.connect(new Node("127.0.0.1", 502));
	}

}
