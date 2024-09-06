package org.tio.core.clickhouse;

import org.tio.client.ReconnConf;
import org.tio.client.TioClient;
import org.tio.client.TioClientConfig;
import org.tio.core.Node;
import org.tio.core.clickhouse.core.ClickHouseClientHandler;
import org.tio.core.clickhouse.core.ClickHouseClientListener;

public class ClickHouseClient {

	public static void main(String[] args) throws Exception {
		// 配置
		TioClientConfig config = new TioClientConfig(new ClickHouseClientHandler(), new ClickHouseClientListener());
		config.setReconnConf(new ReconnConf());
		TioClient tioClient = new TioClient(config);
		tioClient.connect(new Node("127.0.0.1", 9000));
	}

}
