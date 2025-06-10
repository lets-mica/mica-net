package org.tio.http.mcp.server;

import org.tio.http.mcp.schema.McpCallToolResult;
import org.tio.http.mcp.schema.McpTool;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * mcp tool 定义
 *
 * @author L.cm
 */
public class McpToolSpecification {
	private final McpTool tool;
	private final BiFunction<McpServer, Map<String, Object>, McpCallToolResult> call;

	public McpToolSpecification(McpTool tool, BiFunction<McpServer, Map<String, Object>, McpCallToolResult> call) {
		this.tool = tool;
		this.call = call;
	}

	public McpTool getTool() {
		return tool;
	}

	public BiFunction<McpServer, Map<String, Object>, McpCallToolResult> getCall() {
		return call;
	}
}
