package org.tio.http.test;

import org.tio.http.common.HttpConfig;
import org.tio.http.server.HttpServerStarter;

import java.io.IOException;

/**
 * http 测试
 *
 * @author L.cm
 */
public class McpTest {

	public static void main(String[] args) throws IOException {
		HttpConfig httpConfig = new HttpConfig(8081);
		TestMcpHandler mcpHandler = new TestMcpHandler();
		HttpServerStarter httpServerStarter = new HttpServerStarter(httpConfig, mcpHandler);
		httpServerStarter.start();
	}

}
