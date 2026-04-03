package net.dreamlu.mica.net.http.mcp.schema;

import java.util.List;

/**
 * mcp tools 回复
 *
 * @author L.cm
 */
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
