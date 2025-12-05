package org.tio.http.test;

import org.tio.http.common.HttpResponse;
import org.tio.http.common.HttpResponseStatus;
import org.tio.http.server.HttpServerStarter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * http 基准测试
 */
public class HttpBenchmark {

	public static void main(String[] args) throws IOException {
		HttpServerStarter httpServerStarter = new HttpServerStarter(8082, request -> {
			HttpResponse httpResponse = new HttpResponse(request);
			httpResponse.setStatus(HttpResponseStatus.C200);
			httpResponse.setBody("Hello World!".getBytes(StandardCharsets.UTF_8));
			return httpResponse;
		});
		httpServerStarter.start();
	}

}
