package org.tio.http.common.mcp.schema;

/**
 * mcp 取消订阅请求
 *
 * @author L.cm
 */
public class McpUnsubscribeRequest {
	private String uri;

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	@Override
	public String toString() {
		return "McpUnsubscribeRequest{" +
			"uri='" + uri + '\'' +
			'}';
	}
}
