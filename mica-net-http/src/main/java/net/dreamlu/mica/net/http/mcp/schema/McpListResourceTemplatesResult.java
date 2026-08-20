package net.dreamlu.mica.net.http.mcp.schema;

import java.util.List;

/**
 * mcp 资源列表模板回复
 *
 * @author L.cm
 */
public class McpListResourceTemplatesResult {
	private List<McpResourceTemplate> resourceTemplates;
	private String nextCursor;
	/**
	 * 缓存 TTL（毫秒，2026-07-28+）。
	 */
	private Long ttlMs;
	/**
	 * 缓存作用范围（2026-07-28+）。
	 */
	private String cacheScope;

	public List<McpResourceTemplate> getResourceTemplates() {
		return resourceTemplates;
	}

	public void setResourceTemplates(List<McpResourceTemplate> resourceTemplates) {
		this.resourceTemplates = resourceTemplates;
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
		return "McpListResourceTemplatesResult{" +
			"resourceTemplates=" + resourceTemplates +
			", nextCursor='" + nextCursor + '\'' +
			", ttlMs=" + ttlMs +
			", cacheScope='" + cacheScope + '\'' +
			'}';
	}
}
