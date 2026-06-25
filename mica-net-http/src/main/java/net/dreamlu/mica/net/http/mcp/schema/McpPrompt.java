package net.dreamlu.mica.net.http.mcp.schema;

import java.util.List;

/**
 * mcp 提示词
 *
 * @author L.cm
 */
public class McpPrompt implements McpAnnotated {
	private String name;
	private String description;
	private List<McpPromptArgument> arguments;
	/**
	 * 可选注解
	 */
	private McpAnnotations annotations;

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
	public McpAnnotations getAnnotations() {
		return annotations;
	}

	public void setAnnotations(McpAnnotations annotations) {
		this.annotations = annotations;
	}

	@Override
	public String toString() {
		return "McpPrompt{" +
			"name='" + name + '\'' +
			", description='" + description + '\'' +
			", arguments=" + arguments +
			", annotations=" + annotations +
			'}';
	}
}