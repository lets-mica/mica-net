package net.dreamlu.mica.net.http.mcp.schema;

/**
 * Identifies a prompt or resource template by name (or URI template).
 *
 * <p>Exactly one of {@link #name} or {@link #uri} should be set.</p>
 *
 * @author L.cm
 */
public class McpCompleteReference {
	/**
	 * The name of the prompt.
	 */
	private String name;
	/**
	 * The URI template of the resource.
	 */
	private String uri;
	/**
	 * Discriminator: {@code "ref/prompt"} or {@code "ref/resource"}.
	 */
	private String type;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "McpCompleteReference{" +
			"name='" + name + '\'' +
			", uri='" + uri + '\'' +
			", type='" + type + '\'' +
			'}';
	}
}