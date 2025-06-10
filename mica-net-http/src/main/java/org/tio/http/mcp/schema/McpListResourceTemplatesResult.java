package org.tio.http.mcp.schema;

import java.util.List;

/**
 * mcp 资源列表模板回复
 *
 * @author L.cm
 */
public class McpListResourceTemplatesResult {
	private List<McpResourceTemplate> resourceTemplates;
	private String nextCursor;

	public List<McpResourceTemplate> getResourceTemplates() {
		return resourceTemplates;
	}

	public void setResourceTemplates(List<McpResourceTemplate> resourceTemplates) {
		this.resourceTemplates = resourceTemplates;
	}

	public String getNextCursor() {
		return nextCursor;
	}

	public void setNextCursor(String nextCursor) {
		this.nextCursor = nextCursor;
	}

	@Override
	public String toString() {
		return "McpListResourceTemplatesResult{" +
			"resourceTemplates=" + resourceTemplates +
			", nextCursor='" + nextCursor + '\'' +
			'}';
	}
}
