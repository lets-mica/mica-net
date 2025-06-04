package org.tio.http.common.mcp.server;

import org.tio.http.common.mcp.schema.McpImplementation;
import org.tio.http.common.mcp.schema.McpRoot;
import org.tio.http.common.mcp.schema.McpServerCapabilities;
import org.tio.http.common.mcp.util.UriTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * mcp 服务端功能
 *
 * @author L.cm
 */
public class McpServerFeatures {
	private final McpImplementation serverInfo;
	private final McpServerCapabilities serverCapabilities;
	private final List<McpToolSpecification> tools;
	private final Map<String, McpResourceTemplateSpecification> resources;
	private final Map<UriTemplate, McpResourceTemplateSpecification> resourceTemplates;
	private final Map<String, McpPromptSpecification> prompts;
	private final List<BiConsumer<McpServerExchange, List<McpRoot>>> rootsChangeConsumers;

	public McpServerFeatures(McpImplementation serverInfo,
							 McpServerCapabilities serverCapabilities,
							 List<McpToolSpecification> tools,
							 Map<String, McpResourceTemplateSpecification> resources,
							 Map<UriTemplate, McpResourceTemplateSpecification> resourceTemplates,
							 Map<String, McpPromptSpecification> prompts,
							 List<BiConsumer<McpServerExchange, List<McpRoot>>> rootsChangeConsumers) {
		this.serverInfo = serverInfo;
		this.serverCapabilities = serverCapabilities;
		this.tools = tools;
		this.resources = resources;
		this.resourceTemplates = resourceTemplates;
		this.prompts = prompts;
		this.rootsChangeConsumers = rootsChangeConsumers;
	}

	public McpImplementation getServerInfo() {
		return serverInfo;
	}

	public McpServerCapabilities getServerCapabilities() {
		return serverCapabilities;
	}

	public List<McpToolSpecification> getTools() {
		return tools;
	}

	public Map<String, McpResourceTemplateSpecification> getResources() {
		return resources;
	}

	public Map<UriTemplate, McpResourceTemplateSpecification> getResourceTemplates() {
		return resourceTemplates;
	}

	public Map<String, McpPromptSpecification> getPrompts() {
		return prompts;
	}

	public List<BiConsumer<McpServerExchange, List<McpRoot>>> getRootsChangeConsumers() {
		return rootsChangeConsumers;
	}
}
