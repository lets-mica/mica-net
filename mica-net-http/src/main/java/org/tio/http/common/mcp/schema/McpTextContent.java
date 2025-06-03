package org.tio.http.common.mcp.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * mcp 文本内容
 *
 * @author L.cm
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class McpTextContent implements McpContent {
	private List<McpRole> audience;
	private Double priority;
	private String text;

	public McpTextContent() {
	}

	public McpTextContent(String content) {
		this(null, null, content);
	}

	public McpTextContent(List<McpRole> audience, Double priority, String text) {
		this.audience = audience;
		this.priority = priority;
		this.text = text;
	}

	public List<McpRole> getAudience() {
		return audience;
	}

	public void setAudience(List<McpRole> audience) {
		this.audience = audience;
	}

	public Double getPriority() {
		return priority;
	}

	public void setPriority(Double priority) {
		this.priority = priority;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return "McpTextContent{" +
			"audience=" + audience +
			", priority=" + priority +
			", text='" + text + '\'' +
			'}';
	}
}
