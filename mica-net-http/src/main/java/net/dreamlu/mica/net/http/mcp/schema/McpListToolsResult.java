package net.dreamlu.mica.net.http.mcp.schema;

import java.util.List;

/**
 * mcp tools 回复
 *
 * @author L.cm
 */
public class McpListToolsResult {
	private List<McpTool> tools;
	private String nextCursor;
	/**
	 * 缓存 TTL（毫秒，2026-07-28+）。
	 * <p>client 可在 ttlMs 时间内缓存本响应，无需再次调用 tools/list。</p>
	 */
	private Long ttlMs;
	/**
	 * 缓存作用范围（2026-07-28+），例如 "user" / "session" / "global"。
	 */
	private String cacheScope;

	public McpListToolsResult() {
	}

	public McpListToolsResult(List<McpTool> tools, String nextCursor) {
		this.tools = tools;
		this.nextCursor = nextCursor;
	}

	public List<McpTool> getTools() {
		return tools;
	}

	public void setTools(List<McpTool> tools) {
		this.tools = tools;
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
		return "McpListToolsResult{" +
			"tools=" + tools +
			", nextCursor='" + nextCursor + '\'' +
			", ttlMs=" + ttlMs +
			", cacheScope='" + cacheScope + '\'' +
			'}';
	}
}
