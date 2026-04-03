package net.dreamlu.mica.net.http.mcp.schema;

import java.util.List;

/**
 * mcp 文本内容
 *
 * @author L.cm
 */
public class McpTextContent implements McpContent {
	private List<McpRole> audience;
	private Double priority;
	private final String type = "text";
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

	public String getType() {
		return type;
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
			", type='" + type + '\'' +
			", text='" + text + '\'' +
			'}';
	}
}
