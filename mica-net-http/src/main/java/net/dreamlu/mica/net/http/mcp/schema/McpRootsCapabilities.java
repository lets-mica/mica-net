package net.dreamlu.mica.net.http.mcp.schema;

/**
 * Indicates that the client supports listing roots.
 *
 * @author L.cm
 */
public class McpRootsCapabilities {
	private Boolean listChanged;

	public Boolean getListChanged() {
		return listChanged;
	}

	public void setListChanged(Boolean listChanged) {
		this.listChanged = listChanged;
	}

	@Override
	public String toString() {
		return "McpRootsCapabilities{" +
			"listChanged=" + listChanged +
			'}';
	}
}