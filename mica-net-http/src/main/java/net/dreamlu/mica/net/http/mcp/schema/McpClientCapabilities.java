package net.dreamlu.mica.net.http.mcp.schema;

import java.util.Map;

/**
 * mcp client capabilities
 *
 * @author L.cm
 */
public class McpClientCapabilities {
	private Map<String, Object> experimental;
	private McpSamplingCapabilities sampling;
	private McpRootsCapabilities roots;

	public Map<String, Object> getExperimental() {
		return experimental;
	}

	public void setExperimental(Map<String, Object> experimental) {
		this.experimental = experimental;
	}

	public McpSamplingCapabilities getSampling() {
		return sampling;
	}

	public void setSampling(McpSamplingCapabilities sampling) {
		this.sampling = sampling;
	}

	public McpRootsCapabilities getRoots() {
		return roots;
	}

	public void setRoots(McpRootsCapabilities roots) {
		this.roots = roots;
	}

	@Override
	public String toString() {
		return "McpClientCapabilities{" +
			"experimental=" + experimental +
			", sampling=" + sampling +
			", roots=" + roots +
			'}';
	}
}