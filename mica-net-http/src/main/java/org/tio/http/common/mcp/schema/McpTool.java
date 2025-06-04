package org.tio.http.common.mcp.schema;

/**
 * mcp tool
 *
 * @author L.cm
 */
public class McpTool {
	/**
	 * 名字
	 */
	private String name;
	/**
	 * 描述
	 */
	private String description;
	/**
	 * 是否直接返回给调用者
	 */
	private Boolean returnDirect;
	private McpJsonSchema inputSchema;
	private McpJsonSchema outputSchema;

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

	public Boolean getReturnDirect() {
		return returnDirect;
	}

	public void setReturnDirect(Boolean returnDirect) {
		this.returnDirect = returnDirect;
	}

	public McpJsonSchema getInputSchema() {
		return inputSchema;
	}

	public void setInputSchema(McpJsonSchema inputSchema) {
		this.inputSchema = inputSchema;
	}

	public McpJsonSchema getOutputSchema() {
		return outputSchema;
	}

	public void setOutputSchema(McpJsonSchema outputSchema) {
		this.outputSchema = outputSchema;
	}

	@Override
	public String toString() {
		return "McpTool{" +
			"name='" + name + '\'' +
			", description='" + description + '\'' +
			", returnDirect=" + returnDirect +
			", inputSchema=" + inputSchema +
			", outputSchema=" + outputSchema +
			'}';
	}
}
