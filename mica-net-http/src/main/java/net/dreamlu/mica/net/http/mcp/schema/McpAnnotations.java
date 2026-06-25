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

	/**
	 * A human-readable title for the object, suitable for display in UIs.
	 */
	private String title;

	/**
	 * If true, indicates the tool/prompt reads but does not modify its environment.
	 */
	private Boolean readOnlyHint;

	/**
	 * If true, indicates the tool may perform destructive updates (only meaningful when {@link #readOnlyHint} != true).
	 */
	private Boolean destructiveHint;

	/**
	 * If true, indicates calling the tool repeatedly with the same arguments has no additional effect.
	 */
	private Boolean idempotentHint;

	/**
	 * If true, indicates the tool may interact with an "open world" of external entities.
	 */
	private Boolean openWorldHint;

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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Boolean getReadOnlyHint() {
		return readOnlyHint;
	}

	public void setReadOnlyHint(Boolean readOnlyHint) {
		this.readOnlyHint = readOnlyHint;
	}

	public Boolean getDestructiveHint() {
		return destructiveHint;
	}

	public void setDestructiveHint(Boolean destructiveHint) {
		this.destructiveHint = destructiveHint;
	}

	public Boolean getIdempotentHint() {
		return idempotentHint;
	}

	public void setIdempotentHint(Boolean idempotentHint) {
		this.idempotentHint = idempotentHint;
	}

	public Boolean getOpenWorldHint() {
		return openWorldHint;
	}

	public void setOpenWorldHint(Boolean openWorldHint) {
		this.openWorldHint = openWorldHint;
	}

	@Override
	public String toString() {
		return "McpAnnotations{" +
			"audience=" + audience +
			", priority=" + priority +
			", title='" + title + '\'' +
			", readOnlyHint=" + readOnlyHint +
			", destructiveHint=" + destructiveHint +
			", idempotentHint=" + idempotentHint +
			", openWorldHint=" + openWorldHint +
			'}';
	}
}