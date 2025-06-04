package org.tio.http.common.mcp.schema;

/**
 * mcp prompt 参数
 *
 * @author L.cm
 */
public class McpPromptArgument {
	private String name;
	private String description;
	private Boolean required;

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

	public Boolean getRequired() {
		return required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}

	@Override
	public String toString() {
		return "McpPromptArgument{" +
			"name='" + name + '\'' +
			", description='" + description + '\'' +
			", required=" + required +
			'}';
	}
}
