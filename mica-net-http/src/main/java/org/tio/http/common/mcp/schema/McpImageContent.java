package org.tio.http.common.mcp.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * mcp 图片内容
 *
 * @author L.cm
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class McpImageContent implements McpContent {
	private List<McpRole> audience;
	private Double priority;
	private String data;
	private String mimeType;

	public List<McpRole> getAudience() {
		return audience;
	}

	public void setAudience(List<McpRole> audience) {
		this.audience = audience;
	}

	public Double getPriority() {
		return priority;
	}

	public void setPriority(Double priority) {
		this.priority = priority;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	@Override
	public String toString() {
		return "McpImageContent{" +
			"audience=" + audience +
			", priority=" + priority +
			", data='" + data + '\'' +
			", mimeType='" + mimeType + '\'' +
			'}';
	}
}
