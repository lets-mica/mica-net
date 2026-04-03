package net.dreamlu.mica.net.http.mcp.schema;

/**
 * mcp root
 *
 * @author L.cm
 */
public class McpRoot {
	private String uri;
	private String name;

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

	@Override
	public String toString() {
		return "McpRoot{" +
			"uri='" + uri + '\'' +
			", name='" + name + '\'' +
			'}';
	}
}
