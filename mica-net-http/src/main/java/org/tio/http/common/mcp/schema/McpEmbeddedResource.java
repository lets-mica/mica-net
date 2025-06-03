package org.tio.http.common.mcp.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class McpEmbeddedResource implements McpContent {
	private final String type = "resource";

	public String getType() {
		return type;
	}
}
