package net.dreamlu.mica.net.http.test;

import net.dreamlu.mica.net.http.common.HttpRequest;
import net.dreamlu.mica.net.http.common.HttpResponse;
import net.dreamlu.mica.net.http.common.HttpResponseStatus;
import net.dreamlu.mica.net.http.common.RequestLine;
import net.dreamlu.mica.net.http.common.handler.HttpRequestHandler;
import net.dreamlu.mica.net.http.mcp.server.McpServer;

import java.nio.charset.StandardCharsets;

public class TestMcpHandler implements HttpRequestHandler {
	private final McpServer mcpServer;

	public TestMcpHandler(McpServer mcpServer) {
		this.mcpServer = mcpServer;
	}

	@Override
	public HttpResponse handler(HttpRequest request) throws Exception {
		RequestLine requestLine = request.getRequestLine();
		String path = requestLine.getPath();
		System.out.println(path);
		// 使用 transport 统一处理
		HttpResponse response = mcpServer.handleRequest(request);
		if (response.getStatus() != HttpResponseStatus.C404) {
			return response;
		}
		HttpResponse httpResponse = new HttpResponse(request);
		httpResponse.setBody("hello".getBytes(StandardCharsets.UTF_8));
		return httpResponse;
	}

}
