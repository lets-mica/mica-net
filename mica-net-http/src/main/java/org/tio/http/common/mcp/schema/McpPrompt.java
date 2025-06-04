package org.tio.http.common.mcp.schema;

import java.util.List;

/**
 * mcp 提示词
 *
 * @author L.cm
 */
public class McpPrompt {
	private String name;
	private String description;
	private List<McpPromptArgument> arguments;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<McpPromptArgument> getArguments() {
		return arguments;
	}

	public void setArguments(List<McpPromptArgument> arguments) {
		this.arguments = arguments;
	}

	@Override
	public String toString() {
		return "McpPrompt{" +
			"name='" + name + '\'' +
			", description='" + description + '\'' +
			", arguments=" + arguments +
			'}';
	}
}
