package org.tio.http.common.mcp.schema;

import javax.management.relation.Role;
import java.util.List;

/**
 * mcp 文本内容
 *
 * @author L.cm
 */
public class McpTextContent implements McpContent {
	private List<McpRole> audience;
	private Double priority;
	private String text;

	public McpTextContent() {
	}

	public McpTextContent(String content) {
		this(null, null, content);
	}

	public McpTextContent(List<McpRole> audience, Double priority, String text) {
		this.audience = audience;
		this.priority = priority;
		this.text = text;
	}

	@Override
	public String toString() {
		return "McpTextContent{" +
			"audience=" + audience +
			", priority=" + priority +
			", text='" + text + '\'' +
			'}';
	}
}
