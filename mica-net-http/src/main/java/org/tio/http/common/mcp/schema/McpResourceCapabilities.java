package org.tio.http.common.mcp.schema;

/**
 * mcp 资源信息
 *
 * @author L.cm
 */
public class McpResourceCapabilities {
	private Boolean subscribe;
	private Boolean listChanged;

	public Boolean getSubscribe() {
		return subscribe;
	}

	public void setSubscribe(Boolean subscribe) {
		this.subscribe = subscribe;
	}

	public Boolean getListChanged() {
		return listChanged;
	}

	public void setListChanged(Boolean listChanged) {
		this.listChanged = listChanged;
	}

	@Override
	public String toString() {
		return "McpResourceCapabilities{" +
			"subscribe=" + subscribe +
			", listChanged=" + listChanged +
			'}';
	}
}
