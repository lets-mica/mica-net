package org.tio.http.common.mcp.server;

import org.tio.http.common.jsonrpc.JsonRpcMessage;
import org.tio.http.common.sse.SseEmitter;
import org.tio.http.common.sse.SseEvent;
import org.tio.utils.json.JsonUtil;

/**
 * mcp 服务端 session，注意：先写死 sse，没必要整那么复杂，先玩会了后面再调整
 *
 * @author L.cm
 */
public class McpServerSession {
	/**
	 * Event type for JSON-RPC messages sent through the SSE connection.
	 */
	public static final String MESSAGE_EVENT_TYPE = "message";
	private final String sessionId;
	private final SseEmitter sseEmitter;

	public McpServerSession(String sessionId, SseEmitter sseEmitter) {
		this.sessionId = sessionId;
		this.sseEmitter = sseEmitter;
	}

	/**
	 * 发送心跳
	 */
	public void sendHeartbeat() {
		sseEmitter.send(new SseEvent().comment("heartbeat"));
	}

	/**
	 * 发送消息
	 *
	 * @param message JsonRpcMessage
	 */
	public void sendMessage(JsonRpcMessage message) {
		sseEmitter.send(MESSAGE_EVENT_TYPE, JsonUtil.toJsonString(message));
	}

	/**
	 * 关闭
	 */
	public void close() {
		sseEmitter.close();
	}

	@Override
	public String toString() {
		return "McpServerSession{" +
			"sessionId='" + sessionId + '\'' +
			'}';
	}
}
