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
		if ("/sse".equals(path)) {
			// 跨域支持
			return mcpServer.sseEndpoint(request);
		} else if ("/sse/message".equals(path)) {
			return mcpServer.sseMessageEndpoint(request);
		}
		HttpResponse httpResponse = new HttpResponse(request);
		httpResponse.setBody("hello".getBytes(StandardCharsets.UTF_8));
		return httpResponse;
	}

}
