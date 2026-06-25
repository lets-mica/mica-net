package net.dreamlu.mica.net.http.mcp.schema;

/**
 * The severity of a log message.
 *
 * <p>These map to syslog message severities, as described in RFC 5424.</p>
 *
 * @author L.cm
 * @see <a href="https://modelcontextprotocol.io/specification/2025-03-26/server/utilities/logging">MCP logging</a>
 */
public enum McpLoggingLevel {
	/**
	 * Debug-level messages
	 */
	DEBUG,
	/**
	 * Informational messages
	 */
	INFO,
	/**
	 * Normal but significant condition
	 */
	NOTICE,
	/**
	 * Warning conditions
	 */
	WARNING,
	/**
	 * Error conditions
	 */
	ERROR,
	/**
	 * Critical conditions
	 */
	CRITICAL,
	/**
	 * Action must be taken immediately
	 */
	ALERT,
	/**
	 * System is unusable
	 */
	EMERGENCY
}