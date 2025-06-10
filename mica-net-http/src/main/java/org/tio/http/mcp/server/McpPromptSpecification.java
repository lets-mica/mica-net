package org.tio.http.mcp.server;

import org.tio.http.mcp.schema.McpGetPromptRequest;
import org.tio.http.mcp.schema.McpGetPromptResult;
import org.tio.http.mcp.schema.McpPrompt;

import java.util.function.BiFunction;

/**
 * mcp 提示词处理
 *
 * @author L.cm
 */
public class McpPromptSpecification {
	private final McpPrompt prompt;
	private final BiFunction<McpServer, McpGetPromptRequest, McpGetPromptResult> promptHandler;

	public McpPromptSpecification(McpPrompt prompt, BiFunction<McpServer, McpGetPromptRequest, McpGetPromptResult> promptHandler) {
		this.prompt = prompt;
		this.promptHandler = promptHandler;
	}

	public McpPrompt getPrompt() {
		return prompt;
	}

	public BiFunction<McpServer, McpGetPromptRequest, McpGetPromptResult> getPromptHandler() {
		return promptHandler;
	}
}
