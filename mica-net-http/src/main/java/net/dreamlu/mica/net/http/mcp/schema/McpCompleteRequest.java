package net.dreamlu.mica.net.http.mcp.schema;

/**
 * Parameters for the {@code completion/complete} request.
 *
 * <p>Used to request argument auto-completion for prompts or resource templates.</p>
 *
 * @author L.cm
 */
public class McpCompleteRequest {
	/**
	 * The reference to the prompt or resource template being completed.
	 */
	private McpCompleteReference ref;
	/**
	 * The argument's name being completed.
	 */
	private String argument;
	/**
	 * Partial argument value.
	 */
	private String value;

	public McpCompleteReference getRef() {
		return ref;
	}

	public void setRef(McpCompleteReference ref) {
		this.ref = ref;
	}

	public String getArgument() {
		return argument;
	}

	public void setArgument(String argument) {
		this.argument = argument;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "McpCompleteRequest{" +
			"ref=" + ref +
			", argument='" + argument + '\'' +
			", value='" + value + '\'' +
			'}';
	}
}