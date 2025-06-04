package org.tio.http.common.mcp.schema;

import java.util.List;

/**
 * mcp call tool 回复
 *
 * @author L.cm
 */
public class McpCallToolResult {
	private List<McpContent> content;
	private Object structuredContent;
	private Boolean isError;

	public List<McpContent> getContent() {
		return content;
	}

	public void setContent(List<McpContent> content) {
		this.content = content;
	}

	public Object getStructuredContent() {
		return structuredContent;
	}

	public void setStructuredContent(Object structuredContent) {
		this.structuredContent = structuredContent;
	}

	public Boolean getError() {
		return isError;
	}

	public void setError(Boolean error) {
		isError = error;
	}

	@Override
	public String toString() {
		return "McpCallToolResult{" +
			"content=" + content +
			", structuredContent=" + structuredContent +
			", isError=" + isError +
			'}';
	}
}
