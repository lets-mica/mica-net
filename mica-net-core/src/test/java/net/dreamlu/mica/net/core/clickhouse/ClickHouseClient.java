package net.dreamlu.mica.net.core.clickhouse;

import net.dreamlu.mica.net.client.ReconnConf;
import net.dreamlu.mica.net.client.TioClient;
import net.dreamlu.mica.net.client.TioClientConfig;
import net.dreamlu.mica.net.core.Node;
import net.dreamlu.mica.net.core.clickhouse.core.ClickHouseClientHandler;
import net.dreamlu.mica.net.core.clickhouse.core.ClickHouseClientListener;

public class ClickHouseClient {

	public static void main(String[] args) throws Exception {
		// 配置
		TioClientConfig config = new TioClientConfig(new ClickHouseClientHandler(), new ClickHouseClientListener());
		config.setReconnConf(new ReconnConf());
		TioClient tioClient = new TioClient(config);
		tioClient.connect(new Node("127.0.0.1", 9000));
	}

}
