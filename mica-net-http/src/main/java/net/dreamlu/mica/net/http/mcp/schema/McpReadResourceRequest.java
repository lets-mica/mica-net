package net.dreamlu.mica.net.http.mcp.schema;

/**
 * mcp 资源读取请求
 *
 * @author L.cm
 */
public class McpReadResourceRequest {
	private String uri;

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	@Override
	public String toString() {
		return "McpReadResourceRequest{" +
			"uri='" + uri + '\'' +
			'}';
	}
}
