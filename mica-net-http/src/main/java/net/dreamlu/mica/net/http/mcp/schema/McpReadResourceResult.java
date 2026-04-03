package net.dreamlu.mica.net.http.mcp.schema;

import java.util.List;

/**
 * mcp 读取资源回复
 *
 * @author L.cm
 */
public class McpReadResourceResult {
	private List<McpResourceContents> contents;

	public List<McpResourceContents> getContents() {
		return contents;
	}

	public void setContents(List<McpResourceContents> contents) {
		this.contents = contents;
	}

	@Override
	public String toString() {
		return "McpReadResourceResult{" +
			"contents=" + contents +
			'}';
	}
}
