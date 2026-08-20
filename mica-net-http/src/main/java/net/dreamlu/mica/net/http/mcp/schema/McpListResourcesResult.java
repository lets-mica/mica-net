package net.dreamlu.mica.net.http.mcp.schema;

import java.util.List;

/**
 * mcp 列表资源回复
 *
 * @author L.cm
 */
public class McpListResourcesResult {
	private List<McpResource> resources;
	private String nextCursor;
	/**
	 * 缓存 TTL（毫秒，2026-07-28+）。
	 */
	private Long ttlMs;
	/**
	 * 缓存作用范围（2026-07-28+）。
	 */
	private String cacheScope;

	public List<McpResource> getResources() {
		return resources;
	}

	public void setResources(List<McpResource> resources) {
		this.resources = resources;
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
		return "McpListResourcesResult{" +
			"resources=" + resources +
			", nextCursor='" + nextCursor + '\'' +
			", ttlMs=" + ttlMs +
			", cacheScope='" + cacheScope + '\'' +
			'}';
	}
}
