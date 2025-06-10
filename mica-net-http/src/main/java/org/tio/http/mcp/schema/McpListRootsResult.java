package org.tio.http.mcp.schema;

import java.util.List;

public class McpListRootsResult {
	private List<McpRoot> roots;

	public List<McpRoot> getRoots() {
		return roots;
	}

	public void setRoots(List<McpRoot> roots) {
		this.roots = roots;
	}

	@Override
	public String toString() {
		return "McpListRootsResult{" +
			"roots=" + roots +
			'}';
	}
}
