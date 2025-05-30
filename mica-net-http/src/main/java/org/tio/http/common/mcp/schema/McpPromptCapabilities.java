package org.tio.http.common.mcp.schema;

/**
 * mcp 提示词信息
 *
 * @author L.cm
 */
public class McpPromptCapabilities {
	private Boolean listChanged;

	public Boolean getListChanged() {
		return listChanged;
	}

	public void setListChanged(Boolean listChanged) {
		this.listChanged = listChanged;
	}

	@Override
	public String toString() {
		return "McpPromptCapabilities{" +
			"listChanged=" + listChanged +
			'}';
	}
}
