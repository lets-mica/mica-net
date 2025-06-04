package org.tio.http.common.mcp.server;

import org.tio.http.common.mcp.schema.McpImplementation;
import org.tio.http.common.mcp.schema.McpRoot;
import org.tio.http.common.mcp.schema.McpServerCapabilities;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * mcp 服务端功能
 *
 * @author L.cm
 */
public class McpServerFeatures {
	private McpImplementation serverInfo;
	private McpServerCapabilities serverCapabilities;
	List<McpToolSpecification> tools;
//	Map<String, AsyncResourceSpecification> resources;
//	Map<UriTemplate, AsyncResourceTemplateSpecification> resourceTemplates;
	Map<String, McpPromptSpecification> prompts;
	List<BiConsumer<McpServerExchange, List<McpRoot>>> rootsChangeConsumers;

}
