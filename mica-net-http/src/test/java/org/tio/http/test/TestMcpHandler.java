package org.tio.http.test;

import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.http.common.RequestLine;
import org.tio.http.common.handler.HttpRequestHandler;
import org.tio.http.mcp.server.McpServer;

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
		if (response.getStatus() != org.tio.http.common.HttpResponseStatus.C404) {
			return response;
		}
		HttpResponse httpResponse = new HttpResponse(request);
		httpResponse.setBody("hello".getBytes(StandardCharsets.UTF_8));
		return httpResponse;
	}

}
