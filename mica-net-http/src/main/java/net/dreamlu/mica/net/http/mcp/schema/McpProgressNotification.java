package net.dreamlu.mica.net.http.mcp.schema;

/**
 * Parameters for the {@code notifications/progress} notification.
 *
 * <p>Out-of-band progress updates for long-running requests.
 * Server → Client direction supported by MCP.</p>
 *
 * @author L.cm
 */
public class McpProgressNotification {
	/**
	 * The progress token which was given in the initial request, used to associate this notification with that request.
	 */
	private String progressToken;
	/**
	 * The progress value so far. Should increase with each notification.
	 */
	private Double progress;
	/**
	 * Total amount of work to do, if known.
	 */
	private Double total;
	/**
	 * An optional message describing the current progress.
	 */
	private String message;

	public String getProgressToken() {
		return progressToken;
	}

	public void setProgressToken(String progressToken) {
		this.progressToken = progressToken;
	}

	public Double getProgress() {
		return progress;
	}

	public void setProgress(Double progress) {
		this.progress = progress;
	}

	public Double getTotal() {
		return total;
	}

	public void setTotal(Double total) {
		this.total = total;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "McpProgressNotification{" +
			"progressToken='" + progressToken + '\'' +
			", progress=" + progress +
			", total=" + total +
			", message='" + message + '\'' +
			'}';
	}
}