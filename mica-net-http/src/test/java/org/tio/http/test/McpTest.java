package org.tio.http.test;

import org.tio.http.common.HttpConfig;
import org.tio.http.server.HttpServerStarter;
import org.tio.utils.json.*;

import java.io.IOException;

/**
 * http 测试
 *
 * @author L.cm
 */
public class McpTest {

	public static void main(String[] args) throws IOException {
		// 测试 json 工具
//		JsonUtil.getJsonAdapter(new FastJson2JsonAdapter());
//		JsonUtil.getJsonAdapter(new FastJson1JsonAdapter());
//		JsonUtil.getJsonAdapter(new GsonJsonAdapter());
//		JsonUtil.getJsonAdapter(new HuToolJsonAdapter());
		JsonUtil.getJsonAdapter(new Snack3JsonAdapter());
		// 启动 mcp 服务
		HttpConfig httpConfig = new HttpConfig(8081);
		TestMcpHandler mcpHandler = new TestMcpHandler();
		HttpServerStarter httpServerStarter = new HttpServerStarter(httpConfig, mcpHandler);
		httpServerStarter.start();
	}

}
