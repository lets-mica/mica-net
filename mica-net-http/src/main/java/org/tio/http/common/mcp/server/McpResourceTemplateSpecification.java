package org.tio.http.common.mcp.server;

import org.tio.http.common.mcp.schema.McpReadResourceRequest;
import org.tio.http.common.mcp.schema.McpReadResourceResult;
import org.tio.http.common.mcp.schema.McpResourceTemplate;

import java.util.function.BiFunction;

public class McpResourceTemplateSpecification {
	private final McpResourceTemplate resource;
	private final BiFunction<McpServerExchange, McpReadResourceRequest, McpReadResourceResult> readHandler;

	public McpResourceTemplateSpecification(McpResourceTemplate resource,
											BiFunction<McpServerExchange, McpReadResourceRequest, McpReadResourceResult> readHandler) {
		this.resource = resource;
		this.readHandler = readHandler;
	}

	public McpResourceTemplate getResource() {
		return resource;
	}

	public BiFunction<McpServerExchange, McpReadResourceRequest, McpReadResourceResult> getReadHandler() {
		return readHandler;
	}
}
