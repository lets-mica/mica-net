package org.tio.http.common.mcp.server;

import org.tio.http.common.mcp.schema.McpReadResourceRequest;
import org.tio.http.common.mcp.schema.McpReadResourceResult;
import org.tio.http.common.mcp.schema.McpResource;

import java.util.function.BiFunction;

/**
 * 资源定义
 *
 * @author L.cm
 */
public class McpResourceSpecification {
	/**
	 * The resource definition including name, description, and MIME type
	 */
	private McpResource resource;
	/**
	 * The function that handles resource read requests.
	 */
	private BiFunction<McpServer, McpReadResourceRequest, McpReadResourceResult> readHandler;

	public McpResourceSpecification() {
	}

	public McpResourceSpecification(McpResource resource, BiFunction<McpServer, McpReadResourceRequest, McpReadResourceResult> readHandler) {
		this.resource = resource;
		this.readHandler = readHandler;
	}

	public McpResource getResource() {
		return resource;
	}

	public void setResource(McpResource resource) {
		this.resource = resource;
	}

	public BiFunction<McpServer, McpReadResourceRequest, McpReadResourceResult> getReadHandler() {
		return readHandler;
	}

	public void setReadHandler(BiFunction<McpServer, McpReadResourceRequest, McpReadResourceResult> readHandler) {
		this.readHandler = readHandler;
	}
}
