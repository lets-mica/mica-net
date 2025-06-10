package org.tio.http.mcp.schema;

/**
 * mcp init 请求
 *
 * @author L.cm
 */
public class McpInitializeRequest implements McpRequest {
	private String protocolVersion;
	private McpClientCapabilities capabilities;
	private McpImplementation clientInfo;

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(String protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public McpClientCapabilities getCapabilities() {
		return capabilities;
	}

	public void setCapabilities(McpClientCapabilities capabilities) {
		this.capabilities = capabilities;
	}

	public McpImplementation getClientInfo() {
		return clientInfo;
	}

	public void setClientInfo(McpImplementation clientInfo) {
		this.clientInfo = clientInfo;
	}

	@Override
	public String toString() {
		return "McpInitializeRequest{" +
			"protocolVersion='" + protocolVersion + '\'' +
			", capabilities=" + capabilities +
			", clientInfo=" + clientInfo +
			'}';
	}
}
