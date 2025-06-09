package org.tio.http.common.mcp.server;

import java.util.concurrent.ConcurrentHashMap;

public class McpServer {

	/**
	 * Event type for sending the message endpoint URI to clients.
	 */
	public static final String ENDPOINT_EVENT_TYPE = "endpoint";

	/**
	 * Default SSE endpoint path as specified by the MCP transport specification.
	 */
	public static final String DEFAULT_SSE_ENDPOINT = "/sse";

	/**
	 * Map of active client sessions, keyed by session ID.
	 */
	private final ConcurrentHashMap<String, McpServerSession> sessions = new ConcurrentHashMap<>();

	/**
	 * 发送心跳
	 */
	public void sendHeartbeat() {
		for (McpServerSession session : sessions.values()) {
			session.sendHeartbeat();
		}
	}
}
