package net.dreamlu.mica.net.http.test;

import net.dreamlu.mica.net.http.common.HttpResponse;
import net.dreamlu.mica.net.http.common.HttpResponseStatus;
import net.dreamlu.mica.net.http.server.HttpServerStarter;

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
