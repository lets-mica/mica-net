package org.tio.http.test;

import org.tio.http.common.HttpConfig;
import org.tio.http.server.HttpServerStarter;

import java.io.IOException;

/**
 * http 测试
 *
 * @author L.cm
 */
public class HttpTest {

	public static void main(String[] args) throws IOException {
		HttpConfig httpConfig = new HttpConfig();
		TestHttpRequestHandler requestHandler = new TestHttpRequestHandler();
		HttpServerStarter httpServerStarter = new HttpServerStarter(httpConfig, requestHandler);
		httpServerStarter.start(null, 8080);
	}

}
