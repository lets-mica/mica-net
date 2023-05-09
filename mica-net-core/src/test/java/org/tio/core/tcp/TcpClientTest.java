package org.tio.core.tcp;

import org.tio.client.*;
import org.tio.client.intf.TioClientHandler;
import org.tio.core.Node;
import org.tio.core.Tio;
import org.tio.core.intf.EncodedPacket;

import java.nio.charset.StandardCharsets;

public class TcpClientTest {

	public static void main(String[] args) throws Exception {
		// 示例：默认的消息长度
		FixedLengthCodec fixedLengthCodec = new FixedLengthCodec("mica:166130695837".getBytes(StandardCharsets.UTF_8).length);
		TioClientHandler tioHandler = new TestTioClientHandler(fixedLengthCodec);
		// 配置
		TioClientConfig config = new TioClientConfig(tioHandler, new DefaultTioClientListener());
		config.setReconnConf(new ReconnConf());
		TioClient tioClient = new TioClient(config);
		ClientChannelContext connect = tioClient.connect(new Node("127.0.0.1", 502));
		// 示例定时上报消息
		tioClient.schedule(() -> {
			// 默认定长的数据
			String message = "mica:" + System.currentTimeMillis();
			// 使用 Tio 发送数据
			Tio.send(connect, new EncodedPacket(message.getBytes(StandardCharsets.UTF_8)));
		}, 3000);
	}

}
