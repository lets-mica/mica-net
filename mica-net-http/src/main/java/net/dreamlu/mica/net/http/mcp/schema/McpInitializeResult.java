package net.dreamlu.mica.net.http.mcp.schema;

/**
 * mcp 请求回复
 *
 * @author L.cm
 */
public class McpInitializeResult {
	private String protocolVersion;
	private McpServerCapabilities capabilities;
	private McpImplementation serverInfo;
	private String instructions;

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(String protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public McpServerCapabilities getCapabilities() {
		return capabilities;
	}

	public void setCapabilities(McpServerCapabilities capabilities) {
		this.capabilities = capabilities;
	}

	public McpImplementation getServerInfo() {
		return serverInfo;
	}

	public void setServerInfo(McpImplementation serverInfo) {
		this.serverInfo = serverInfo;
	}

	public String getInstructions() {
		return instructions;
	}

	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}

	@Override
	public String toString() {
		return "McpInitializeResult{" +
			"protocolVersion='" + protocolVersion + '\'' +
			", capabilities=" + capabilities +
			", serverInfo=" + serverInfo +
			", instructions='" + instructions + '\'' +
			'}';
	}
}
