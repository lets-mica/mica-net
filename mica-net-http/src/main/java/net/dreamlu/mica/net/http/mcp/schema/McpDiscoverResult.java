package net.dreamlu.mica.net.http.mcp.schema;

/**
 * 2026-07-28 server/discover 响应。
 *
 * <p>modern 协议要求每个 server 必须实现 {@code server/discover}，
 * 用于声明自身支持的协议版本、capabilities 与 identity。</p>
 *
 * @author L.cm
 */
public class McpDiscoverResult {
	/**
	 * server 支持的协议版本列表（按优先级降序），供 client 在不兼容时回退。
	 */
	private java.util.List<String> supportedProtocolVersions;
	/**
	 * server capabilities
	 */
	private McpServerCapabilities capabilities;
	/**
	 * server 实现信息
	 */
	private McpImplementation serverInfo;

	public java.util.List<String> getSupportedProtocolVersions() {
		return supportedProtocolVersions;
	}

	public void setSupportedProtocolVersions(java.util.List<String> supportedProtocolVersions) {
		this.supportedProtocolVersions = supportedProtocolVersions;
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

	@Override
	public String toString() {
		return "McpDiscoverResult{" +
			"supportedProtocolVersions=" + supportedProtocolVersions +
			", capabilities=" + capabilities +
			", serverInfo=" + serverInfo +
			'}';
	}
}