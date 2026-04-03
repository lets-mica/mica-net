package net.dreamlu.mica.net.core.tcp;

import net.dreamlu.mica.net.client.*;
import net.dreamlu.mica.net.client.intf.TioClientHandler;
import net.dreamlu.mica.net.core.Node;
import net.dreamlu.mica.net.core.Tio;
import net.dreamlu.mica.net.core.intf.EncodedPacket;

import java.nio.charset.StandardCharsets;

public class TcpClientTest {

	public static void main(String[] args) throws Exception {
		int length = ("mica:" + System.nanoTime()).getBytes(StandardCharsets.UTF_8).length;
		// 示例：默认的消息长度
		FixedLengthCodec fixedLengthCodec = new FixedLengthCodec(length);
		TioClientHandler tioHandler = new TestTioClientHandler(fixedLengthCodec);
		// 配置
		TioClientConfig config = new TioClientConfig(tioHandler, new DefaultTioClientListener());
		config.setReconnConf(new ReconnConf());
		TioClient tioClient = new TioClient(config);
		config.debug = true;
		ClientChannelContext context = tioClient.connect(new Node("127.0.0.1", 502));
		// 示例定时上报消息
		tioClient.schedule(() -> {
			for (int i = 0; i < 1000; i++) {
				// 默认定长的数据
				String message = "mica:" + System.nanoTime();
				// 使用 Tio 发送数据
				Tio.send(context, new EncodedPacket(message.getBytes(StandardCharsets.UTF_8)));
			}
		}, 3000);
	}

}
