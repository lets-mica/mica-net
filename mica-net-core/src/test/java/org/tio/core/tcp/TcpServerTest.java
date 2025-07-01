package org.tio.core.tcp;

import org.tio.server.DefaultTioServerListener;
import org.tio.server.TioServer;
import org.tio.server.TioServerConfig;
import org.tio.server.intf.TioServerHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TcpServerTest {

	public static void main(String[] args) throws IOException {
		// 示例：默认的消息长度
		FixedLengthCodec fixedLengthCodec = new FixedLengthCodec("mica:166130695837".getBytes(StandardCharsets.UTF_8).length);
		TioServerHandler serverHandler = new TestTioServerHandler(fixedLengthCodec);
		// 配置
		TioServerConfig config = new TioServerConfig(serverHandler, new DefaultTioServerListener());
		config.debug = true;
		TioServer tioServer = new TioServer(config, 502);
		tioServer.start();
	}

}
