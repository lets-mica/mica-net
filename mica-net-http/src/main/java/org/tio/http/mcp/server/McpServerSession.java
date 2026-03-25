package org.tio.http.mcp.server;

import org.tio.http.common.stream.HttpStream;
import org.tio.http.jsonrpc.JsonRpcMessage;
import org.tio.utils.json.JsonUtil;

/**
 * mcp 服务端 session
 *
 * @author L.cm
 */
public class McpServerSession {
	/**
	 * Event type for JSON-RPC messages sent through the SSE connection.
	 */
	public static final String MESSAGE_EVENT_TYPE = "message";
	private final String sessionId;
	private final HttpStream httpStream;

	public McpServerSession(String sessionId, HttpStream httpStream) {
		this.sessionId = sessionId;
		this.httpStream = httpStream;
	}

	/**
	 * 发送心跳
	 */
	public void sendHeartbeat() {
		httpStream.send(null, null, "heartbeat");
	}

	/**
	 * 发送消息
	 *
	 * @param message JsonRpcMessage
	 */
	public void sendMessage(JsonRpcMessage message) {
		httpStream.send(MESSAGE_EVENT_TYPE, JsonUtil.toJsonString(message));
	}

	/**
	 * 关闭
	 */
	public void close() {
		httpStream.end();
	}

	@Override
	public String toString() {
		return "McpServerSession{" +
			"sessionId='" + sessionId + '\'' +
			'}';
	}
}
