package net.dreamlu.mica.net.http.mcp.server;

import net.dreamlu.mica.net.http.mcp.schema.McpReadResourceRequest;
import net.dreamlu.mica.net.http.mcp.schema.McpReadResourceResult;
import net.dreamlu.mica.net.http.mcp.schema.McpResourceTemplate;

import java.util.function.BiFunction;

public class McpResourceTemplateSpecification {
	private final McpResourceTemplate resource;
	private final BiFunction<McpServerSession, McpReadResourceRequest, McpReadResourceResult> readHandler;

	public McpResourceTemplateSpecification(McpResourceTemplate resource,
	                                        BiFunction<McpServerSession, McpReadResourceRequest, McpReadResourceResult> readHandler) {
		this.resource = resource;
		this.readHandler = readHandler;
	}

	public McpResourceTemplate getResource() {
		return resource;
	}

	public BiFunction<McpServerSession, McpReadResourceRequest, McpReadResourceResult> getReadHandler() {
		return readHandler;
	}
}
