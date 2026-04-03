package net.dreamlu.mica.net.http.mcp.schema;

/**
 * mcp root 信息
 *
 * @author L.cm
 */
public class McpRootCapabilities {
	private Boolean listChanged;

	public Boolean getListChanged() {
		return listChanged;
	}

	public void setListChanged(Boolean listChanged) {
		this.listChanged = listChanged;
	}

	@Override
	public String toString() {
		return "McpRootCapabilities{" +
			"listChanged=" + listChanged +
			'}';
	}
}
