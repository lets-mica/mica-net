package org.tio.http.common.mcp.server;

import org.tio.http.common.mcp.schema.McpCallToolResult;
import org.tio.http.common.mcp.schema.McpTool;

import java.util.Map;
import java.util.function.BiFunction;

public class McpToolSpecification {
	private final McpTool tool;
	private final BiFunction<McpServerExchange, Map<String, Object>, McpCallToolResult> call;

	public McpToolSpecification(McpTool tool, BiFunction<McpServerExchange, Map<String, Object>, McpCallToolResult> call) {
		this.tool = tool;
		this.call = call;
	}

	public McpTool getTool() {
		return tool;
	}

	public BiFunction<McpServerExchange, Map<String, Object>, McpCallToolResult> getCall() {
		return call;
	}
}
