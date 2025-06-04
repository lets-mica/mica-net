package org.tio.http.common.mcp.server;

import org.tio.http.common.mcp.schema.McpGetPromptRequest;
import org.tio.http.common.mcp.schema.McpGetPromptResult;
import org.tio.http.common.mcp.schema.McpPrompt;

import java.util.function.BiFunction;

/**
 * mcp 提示词处理
 *
 * @author L.cm
 */
public class McpPromptSpecification {
	private final McpPrompt prompt;
	private final BiFunction<McpServerExchange, McpGetPromptRequest, McpGetPromptResult> promptHandler;

	public McpPromptSpecification(McpPrompt prompt, BiFunction<McpServerExchange, McpGetPromptRequest, McpGetPromptResult> promptHandler) {
		this.prompt = prompt;
		this.promptHandler = promptHandler;
	}

	public McpPrompt getPrompt() {
		return prompt;
	}

	public BiFunction<McpServerExchange, McpGetPromptRequest, McpGetPromptResult> getPromptHandler() {
		return promptHandler;
	}
}
