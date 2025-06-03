package org.tio.http.common.mcp.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * mcp tools 回复
 *
 * @author L.cm
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class McpListToolsResult {
	private List<McpTool> tools;
	private String nextCursor;

	public McpListToolsResult() {
	}

	public McpListToolsResult(List<McpTool> tools, String nextCursor) {
		this.tools = tools;
		this.nextCursor = nextCursor;
	}

	public List<McpTool> getTools() {
		return tools;
	}

	public void setTools(List<McpTool> tools) {
		this.tools = tools;
	}

	public String getNextCursor() {
		return nextCursor;
	}

	public void setNextCursor(String nextCursor) {
		this.nextCursor = nextCursor;
	}

	@Override
	public String toString() {
		return "McpListToolsResult{" +
			"tools=" + tools +
			", nextCursor='" + nextCursor + '\'' +
			'}';
	}
}
