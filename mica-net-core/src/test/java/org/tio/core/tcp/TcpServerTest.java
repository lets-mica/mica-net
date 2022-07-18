package org.tio.core.tcp;

import org.tio.server.DefaultTioServerListener;
import org.tio.server.TioServer;
import org.tio.server.TioServerConfig;
import org.tio.server.intf.TioServerHandler;

import java.io.IOException;

public class TcpServerTest {

	public static void main(String[] args) throws IOException {
		TioServerHandler serverHandler = new TestTioServerHandler();
		TioServerConfig config = new TioServerConfig(serverHandler, new DefaultTioServerListener());
		TioServer tioServer = new TioServer(config);
		tioServer.start("0.0.0.0", 502);
	}

}
