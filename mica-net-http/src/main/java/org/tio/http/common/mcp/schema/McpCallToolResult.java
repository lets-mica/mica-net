package org.tio.http.common.mcp.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * mcp call tool 回复
 *
 * @author L.cm
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class McpCallToolResult {
	private List<McpContent> content;
	private Boolean isError;

	public List<McpContent> getContent() {
		return content;
	}

	public void setContent(List<McpContent> content) {
		this.content = content;
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
			", isError=" + isError +
			'}';
	}
}
