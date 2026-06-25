package net.dreamlu.mica.net.http.mcp.schema;

/**
 * Parameters for the {@code notifications/cancelled} notification.
 *
 * <p>Sent by either side to indicate that a previously-issued request is being cancelled.</p>
 *
 * @author L.cm
 */
public class McpCancelledNotification {
	/**
	 * The ID of the request to cancel. This must correspond to the ID of a request
	 * previously issued in the same direction.
	 */
	private Object requestId;
	/**
	 * An optional string describing the reason for the cancellation.
	 */
	private String reason;

	public Object getRequestId() {
		return requestId;
	}

	public void setRequestId(Object requestId) {
		this.requestId = requestId;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@Override
	public String toString() {
		return "McpCancelledNotification{" +
			"requestId=" + requestId +
			", reason='" + reason + '\'' +
			'}';
	}
}