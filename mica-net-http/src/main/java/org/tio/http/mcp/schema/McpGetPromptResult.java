package org.tio.http.mcp.schema;

import java.util.List;

/**
 * mcp 提示词回复
 *
 * @author L.cm
 */
public class McpGetPromptResult {
	private String description;
	private List<McpPromptMessage> messages;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<McpPromptMessage> getMessages() {
		return messages;
	}

	public void setMessages(List<McpPromptMessage> messages) {
		this.messages = messages;
	}

	@Override
	public String toString() {
		return "McpGetPromptResult{" +
			"description='" + description + '\'' +
			", messages=" + messages +
			'}';
	}
}
