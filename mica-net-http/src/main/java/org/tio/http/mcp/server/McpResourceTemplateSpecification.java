package org.tio.http.mcp.server;

import org.tio.http.mcp.schema.McpReadResourceRequest;
import org.tio.http.mcp.schema.McpReadResourceResult;
import org.tio.http.mcp.schema.McpResourceTemplate;

import java.util.function.BiFunction;

public class McpResourceTemplateSpecification {
	private final McpResourceTemplate resource;
	private final BiFunction<McpServer, McpReadResourceRequest, McpReadResourceResult> readHandler;

	public McpResourceTemplateSpecification(McpResourceTemplate resource,
											BiFunction<McpServer, McpReadResourceRequest, McpReadResourceResult> readHandler) {
		this.resource = resource;
		this.readHandler = readHandler;
	}

	public McpResourceTemplate getResource() {
		return resource;
	}

	public BiFunction<McpServer, McpReadResourceRequest, McpReadResourceResult> getReadHandler() {
		return readHandler;
	}
}
