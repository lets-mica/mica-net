package net.dreamlu.mica.net.http.mcp.schema;

/**
 * mcp 提示词消息
 *
 * @author L.cm
 */
public class McpPromptMessage {
	private McpRole role;
	private McpContent content;

	public McpRole getRole() {
		return role;
	}

	public void setRole(McpRole role) {
		this.role = role;
	}

	public McpContent getContent() {
		return content;
	}

	public void setContent(McpContent content) {
		this.content = content;
	}

	@Override
	public String toString() {
		return "McpPromptMessage{" +
			"role=" + role +
			", content=" + content +
			'}';
	}
}
