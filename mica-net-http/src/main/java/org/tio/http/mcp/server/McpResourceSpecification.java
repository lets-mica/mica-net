package org.tio.http.mcp.server;

import org.tio.http.mcp.schema.McpReadResourceRequest;
import org.tio.http.mcp.schema.McpReadResourceResult;
import org.tio.http.mcp.schema.McpResource;

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
	private BiFunction<McpServerSession, McpReadResourceRequest, McpReadResourceResult> readHandler;

	public McpResourceSpecification() {
	}

	public McpResourceSpecification(McpResource resource, BiFunction<McpServerSession, McpReadResourceRequest, McpReadResourceResult> readHandler) {
		this.resource = resource;
		this.readHandler = readHandler;
	}

	public McpResource getResource() {
		return resource;
	}

	public void setResource(McpResource resource) {
		this.resource = resource;
	}

	public BiFunction<McpServerSession, McpReadResourceRequest, McpReadResourceResult> getReadHandler() {
		return readHandler;
	}

	public void setReadHandler(BiFunction<McpServerSession, McpReadResourceRequest, McpReadResourceResult> readHandler) {
		this.readHandler = readHandler;
	}
}
