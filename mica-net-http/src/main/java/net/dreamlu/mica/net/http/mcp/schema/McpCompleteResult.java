package net.dreamlu.mica.net.http.mcp.schema;

import java.util.List;

/**
 * Result for the {@code completion/complete} request.
 *
 * @author L.cm
 */
public class McpCompleteResult {
	/**
	 * Suggested completion values (max 100, per MCP spec).
	 */
	private List<String> values;
	/**
	 * The total number of completion candidates available.
	 */
	private Integer total;
	/**
	 * Indicates whether there are additional completion options beyond those provided.
	 */
	private Boolean hasMore;

	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}

	public Integer getTotal() {
		return total;
	}

	public void setTotal(Integer total) {
		this.total = total;
	}

	public Boolean getHasMore() {
		return hasMore;
	}

	public void setHasMore(Boolean hasMore) {
		this.hasMore = hasMore;
	}

	@Override
	public String toString() {
		return "McpCompleteResult{" +
			"values=" + values +
			", total=" + total +
			", hasMore=" + hasMore +
			'}';
	}
}