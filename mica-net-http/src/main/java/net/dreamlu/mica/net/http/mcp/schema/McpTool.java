package net.dreamlu.mica.net.http.mcp.schema;

/**
 * mcp tool
 *
 * @author L.cm
 */
public class McpTool implements McpAnnotated {
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
	/**
	 * 可选注解
	 */
	private McpAnnotations annotations;
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

	@Override
	public McpAnnotations getAnnotations() {
		return annotations;
	}

	public void setAnnotations(McpAnnotations annotations) {
		this.annotations = annotations;
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
			", annotations=" + annotations +
			", inputSchema=" + inputSchema +
			", outputSchema=" + outputSchema +
			'}';
	}
}