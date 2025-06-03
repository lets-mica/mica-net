package org.tio.http.common.mcp.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * mcp 服务信息
 *
 * @author L.cm
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class McpServerCapabilities {
	private Map<String, Object> experimental;
	private McpLoggingCapabilities logging;
	private McpPromptCapabilities prompts;
	private McpResourceCapabilities resources;
	private McpToolCapabilities tools;

	public Map<String, Object> getExperimental() {
		return experimental;
	}

	public void setExperimental(Map<String, Object> experimental) {
		this.experimental = experimental;
	}

	public McpLoggingCapabilities getLogging() {
		return logging;
	}

	public void setLogging(McpLoggingCapabilities logging) {
		this.logging = logging;
	}

	public McpPromptCapabilities getPrompts() {
		return prompts;
	}

	public void setPrompts(McpPromptCapabilities prompts) {
		this.prompts = prompts;
	}

	public McpResourceCapabilities getResources() {
		return resources;
	}

	public void setResources(McpResourceCapabilities resources) {
		this.resources = resources;
	}

	public McpToolCapabilities getTools() {
		return tools;
	}

	public void setTools(McpToolCapabilities tools) {
		this.tools = tools;
	}

	@Override
	public String toString() {
		return "McpServerCapabilities{" +
			"experimental=" + experimental +
			", logging=" + logging +
			", prompts=" + prompts +
			", resources=" + resources +
			", tools=" + tools +
			'}';
	}
}
