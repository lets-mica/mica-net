package org.tio.http.mcp.schema;

import java.util.List;

/**
 * mcp 列表资源回复
 *
 * @author L.cm
 */
public class McpListResourcesResult {
	private List<McpResource> resources;
	private String nextCursor;

	public List<McpResource> getResources() {
		return resources;
	}

	public void setResources(List<McpResource> resources) {
		this.resources = resources;
	}

	public String getNextCursor() {
		return nextCursor;
	}

	public void setNextCursor(String nextCursor) {
		this.nextCursor = nextCursor;
	}

	@Override
	public String toString() {
		return "McpListResourcesResult{" +
			"resources=" + resources +
			", nextCursor='" + nextCursor + '\'' +
			'}';
	}
}
