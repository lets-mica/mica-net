package org.tio.http.mcp.schema;

/**
 * A known resource that the server is capable of reading.
 *
 * @author L.cm
 */
public class McpResource implements McpAnnotated {
	/**
	 * the URI of the resource.
	 */
	private String uri;
	/**
	 * A human-readable name for this resource. This can be used by clients to populate UI elements.
	 */
	private String name;
	/**
	 * A description of what this resource represents.
	 * This can be used by clients to improve the LLM's understanding of available resources.
	 * It can be thought of like a "hint" to the model.
	 */
	private String description;
	/**
	 * The MIME type of this resource, if known.
	 */
	private String mimeType;
	/**
	 * Optional annotations for the client. The client can use annotations to inform how objects are used or displayed.
	 */
	private McpAnnotations annotations;

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

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

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
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
		return "McpResource{" +
			"uri='" + uri + '\'' +
			", name='" + name + '\'' +
			", description='" + description + '\'' +
			", mimeType='" + mimeType + '\'' +
			", annotations=" + annotations +
			'}';
	}
}
