package org.tio.http.test;

import org.tio.http.server.HttpServerStarter;

import java.io.IOException;

/**
 * http 测试
 *
 * @author L.cm
 */
public class HttpTest {

	public static void main(String[] args) throws IOException {
		TestHttpRequestHandler requestHandler = new TestHttpRequestHandler();
		HttpServerStarter httpServerStarter = new HttpServerStarter(8080, requestHandler);
		httpServerStarter.start();
	}

}
