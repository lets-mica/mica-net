package org.tio.core.tcp;

import org.tio.client.DefaultTioClientListener;
import org.tio.client.TioClient;
import org.tio.client.TioClientConfig;
import org.tio.client.intf.TioClientHandler;
import org.tio.client.intf.TioClientListener;
import org.tio.core.Node;

public class TcpClientTest {

	public static void main(String[] args) throws Exception {
		TioClientHandler tioHandler = new TestTioClientHandler();
		TioClientListener tioListener = new DefaultTioClientListener();
		TioClientConfig config = new TioClientConfig(tioHandler, tioListener);
		TioClient tioClient = new TioClient(config);
		tioClient.connect(new Node("127.0.0.1", 502));
	}

}
