package net.dreamlu.mica.net.http.mcp.schema;

public class McpEmbeddedResource implements McpContent {
	private final String type = "resource";

	public String getType() {
		return type;
	}
}
