package org.tio.http.mcp.schema;

/**
 * mcp tool 信息
 *
 * @author L.cm
 */
public class McpToolCapabilities {
	private Boolean listChanged;

	public Boolean getListChanged() {
		return listChanged;
	}

	public void setListChanged(Boolean listChanged) {
		this.listChanged = listChanged;
	}

	@Override
	public String toString() {
		return "McpToolCapabilities{" +
			"listChanged=" + listChanged +
			'}';
	}
}
