package org.tio.http.common.mcp.schema;

import java.util.Map;

/**
 * mcp call tool 请求
 *
 * @author L.cm
 */
public class McpCallToolRequest implements McpRequest {
	private String name;
	private Map<String, Object> arguments;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, Object> getArguments() {
		return arguments;
	}

	public void setArguments(Map<String, Object> arguments) {
		this.arguments = arguments;
	}

	@Override
	public String toString() {
		return "McpCallToolRequest{" +
			"name='" + name + '\'' +
			", arguments=" + arguments +
			'}';
	}
}
