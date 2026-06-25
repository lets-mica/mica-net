package net.dreamlu.mica.net.http.mcp.schema;

/**
 * Parameters for the {@code logging/setLevel} request from the client.
 *
 * @author L.cm
 */
public class McpSetLevelRequest implements McpRequest {
	/**
	 * The level of logging that the client wants to receive from the server.
	 * The server should send all logs at this level and higher (i.e., more severe)
	 * to the client as notifications/message.
	 */
	private McpLoggingLevel level;

	public McpLoggingLevel getLevel() {
		return level;
	}

	public void setLevel(McpLoggingLevel level) {
		this.level = level;
	}

	@Override
	public String toString() {
		return "McpSetLevelRequest{" +
			"level=" + level +
			'}';
	}
}