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
	/**
	 * 缓存 TTL（毫秒，2026-07-28+）。
	 */
	private Long ttlMs;
	/**
	 * 缓存作用范围（2026-07-28+）。
	 */
	private String cacheScope;

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

	public Long getTtlMs() {
		return ttlMs;
	}

	public void setTtlMs(Long ttlMs) {
		this.ttlMs = ttlMs;
	}

	public String getCacheScope() {
		return cacheScope;
	}

	public void setCacheScope(String cacheScope) {
		this.cacheScope = cacheScope;
	}

	@Override
	public String toString() {
		return "McpListPromptsResult{" +
			"prompts=" + prompts +
			", nextCursor='" + nextCursor + '\'' +
			", ttlMs=" + ttlMs +
			", cacheScope='" + cacheScope + '\'' +
			'}';
	}
}