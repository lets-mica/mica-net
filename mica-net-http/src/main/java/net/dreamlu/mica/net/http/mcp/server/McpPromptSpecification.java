package net.dreamlu.mica.net.http.mcp.server;

import net.dreamlu.mica.net.http.mcp.schema.McpGetPromptRequest;
import net.dreamlu.mica.net.http.mcp.schema.McpGetPromptResult;
import net.dreamlu.mica.net.http.mcp.schema.McpPrompt;

import java.util.function.BiFunction;

/**
 * mcp 提示词处理
 *
 * @author L.cm
 */
public class McpPromptSpecification {
	private final McpPrompt prompt;
	private final BiFunction<McpServerSession, McpGetPromptRequest, McpGetPromptResult> promptHandler;

	public McpPromptSpecification(McpPrompt prompt, BiFunction<McpServerSession, McpGetPromptRequest, McpGetPromptResult> promptHandler) {
		this.prompt = prompt;
		this.promptHandler = promptHandler;
	}

	public McpPrompt getPrompt() {
		return prompt;
	}

	public BiFunction<McpServerSession, McpGetPromptRequest, McpGetPromptResult> getPromptHandler() {
		return promptHandler;
	}
}
