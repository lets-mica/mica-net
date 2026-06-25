package net.dreamlu.mica.net.http.mcp.schema;

import java.util.List;

/**
 * The server's response to a prompts/list request from the client.
 *
 * @author L.cm
 */
public class McpListPromptsResult {
	private List<McpPrompt> prompts;
	private String nextCursor;

	public List<McpPrompt> getPrompts() {
		return prompts;
	}

	public void setPrompts(List<McpPrompt> prompts) {
		this.prompts = prompts;
	}

	public String getNextCursor() {
		return nextCursor;
	}

	public void setNextCursor(String nextCursor) {
		this.nextCursor = nextCursor;
	}

	@Override
	public String toString() {
		return "McpListPromptsResult{" +
			"prompts=" + prompts +
			", nextCursor='" + nextCursor + '\'' +
			'}';
	}
}