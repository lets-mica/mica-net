package net.dreamlu.mica.net.http.mcp.schema;

import java.util.List;

/**
 * Optional annotations for the client. The client can use annotations to inform how
 * objects are used or displayed.
 *
 * @author L.cm
 */
public class McpAnnotations {

	/**
	 * Describes who the intended customer of this object or data is.
	 * It can include multiple entries to indicate content useful for multiple audiences (e.g., `["user", "assistant"]`).
	 */
	private List<McpRole> audience;

	/**
	 * Describes how important this data is for operating the server.
	 * A value of 1 means "most important," and indicates that the data is effectively required,
	 * while 0 means "least important," and indicates that the data is entirely optional.
	 * It is a number between 0 and 1.
	 */
	private Double priority;

	public List<McpRole> getAudience() {
		return audience;
	}

	public void setAudience(List<McpRole> audience) {
		this.audience = audience;
	}

	public Double getPriority() {
		return priority;
	}

	public void setPriority(Double priority) {
		this.priority = priority;
	}

	@Override
	public String toString() {
		return "McpAnnotations{" +
			"audience=" + audience +
			", priority=" + priority +
			'}';
	}
}
