package net.dreamlu.mica.net.http.mcp.schema;

/**
 * Sent from the client to request resources/updated notifications from the server
 * whenever a particular resource changes.
 *
 * @author L.cm
 */
public class McpSubscribeRequest {
	/**
	 * the URI of the resource to subscribe to. The URI can use any protocol;
	 * it is up to the server how to interpret it.
	 */
	private String uri;
}
