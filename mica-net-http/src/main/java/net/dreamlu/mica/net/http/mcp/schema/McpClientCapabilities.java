package net.dreamlu.mica.net.http.mcp.schema;

import java.util.Map;

/**
 * mcp 客户端信息
 *
 * @author L.cm
 */
public class McpClientCapabilities {
	private Map<String, Object> experimental;
	private McpRootCapabilities roots;
	private McpSampling sampling;

	public Map<String, Object> getExperimental() {
		return experimental;
	}

	public void setExperimental(Map<String, Object> experimental) {
		this.experimental = experimental;
	}

	public McpRootCapabilities getRoots() {
		return roots;
	}

	public void setRoots(McpRootCapabilities roots) {
		this.roots = roots;
	}

	public McpSampling getSampling() {
		return sampling;
	}

	public void setSampling(McpSampling sampling) {
		this.sampling = sampling;
	}

	@Override
	public String toString() {
		return "McpClientCapabilities{" +
			"experimental=" + experimental +
			", roots=" + roots +
			", sampling=" + sampling +
			'}';
	}
}
