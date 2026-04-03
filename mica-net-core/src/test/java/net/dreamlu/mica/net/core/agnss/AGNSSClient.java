package net.dreamlu.mica.net.core.agnss;

import net.dreamlu.mica.net.client.ReconnConf;
import net.dreamlu.mica.net.client.TioClient;
import net.dreamlu.mica.net.client.TioClientConfig;
import net.dreamlu.mica.net.core.Node;

public class AGNSSClient {

	public static void main(String[] args) throws Exception {
		AGNSSCodec agnssCodec = new AGNSSCodec();
		// 示例：默认的消息长度
		AGNSSClientHandler tioHandler = new AGNSSClientHandler(agnssCodec);
		// 配置
		TioClientConfig config = new TioClientConfig(tioHandler, new AGNSSClientListener());
		config.setReconnConf(new ReconnConf());
		TioClient tioClient = new TioClient(config);
		tioClient.connect(new Node("121.41.40.95", 2621));
	}

}
