package net.dreamlu.mica.net.core.tcp;

import net.dreamlu.mica.net.server.DefaultTioServerListener;
import net.dreamlu.mica.net.server.TioServer;
import net.dreamlu.mica.net.server.TioServerConfig;
import net.dreamlu.mica.net.server.intf.TioServerHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TcpServerTest {

	public static void main(String[] args) throws IOException {
		int length = ("mica:" + System.nanoTime()).getBytes(StandardCharsets.UTF_8).length;
		// 示例：默认的消息长度
		FixedLengthCodec fixedLengthCodec = new FixedLengthCodec(length);
		TioServerHandler serverHandler = new TestTioServerHandler(fixedLengthCodec);
		// 配置
		TioServerConfig config = new TioServerConfig(serverHandler, new DefaultTioServerListener());
		config.debug = true;
		TioServer tioServer = new TioServer(502, config);
		tioServer.start();
	}

}
