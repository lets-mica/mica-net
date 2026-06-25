package net.dreamlu.mica.net.http.mcp.schema;

import java.util.Map;

/**
 * Parameters for the {@code notifications/message} notification from the server.
 *
 * <p>Used by servers to send log messages to clients.</p>
 *
 * @author L.cm
 */
public class McpLoggingMessageNotification {
	/**
	 * The severity of the log message
	 */
	private McpLoggingLevel level;
	/**
	 * An optional name of the logger issuing the message
	 */
	private String logger;
	/**
	 * The data to be logged, such as a string or structured JSON
	 */
	private Object data;
	/**
	 * Optional additional metadata
	 */
	private Map<String, Object> meta;

	public McpLoggingLevel getLevel() {
		return level;
	}

	public void setLevel(McpLoggingLevel level) {
		this.level = level;
	}

	public String getLogger() {
		return logger;
	}

	public void setLogger(String logger) {
		this.logger = logger;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public Map<String, Object> getMeta() {
		return meta;
	}

	public void setMeta(Map<String, Object> meta) {
		this.meta = meta;
	}

	@Override
	public String toString() {
		return "McpLoggingMessageNotification{" +
			"level=" + level +
			", logger='" + logger + '\'' +
			", data=" + data +
			", meta=" + meta +
			'}';
	}
}