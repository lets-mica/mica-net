package net.dreamlu.mica.net.http.mcp.server.transport;

import net.dreamlu.mica.net.http.common.HttpRequest;
import net.dreamlu.mica.net.http.common.HttpResponse;
import net.dreamlu.mica.net.http.common.HttpResponseStatus;
import net.dreamlu.mica.net.http.common.RequestLine;
import net.dreamlu.mica.net.http.common.stream.HttpStream;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcErrorCodes;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcMessage;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcNotification;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcRequest;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcResponse;
import net.dreamlu.mica.net.http.mcp.server.McpServer;
import net.dreamlu.mica.net.http.mcp.server.McpServerSession;
import net.dreamlu.mica.net.utils.hutool.StrUtil;
import net.dreamlu.mica.net.utils.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * MCP SSE Transport 实现。
 *
 * <p>采用两个端点：
 * <ul>
 *   <li>GET {sseEndpoint} - 建立 SSE 连接，立即分配 sessionId 并通过 endpoint 事件下发</li>
 *   <li>POST {messageEndpoint}?sessionId=xxx - 发送 JSON-RPC 消息</li>
 * </ul>
 *
 * @author L.cm
 */
public class SseTransport implements McpTransport {
	public static final String TRANSPORT_TYPE = "sse";
	public static final String DEFAULT_SSE_ENDPOINT = "/sse";
	public static final String DEFAULT_MESSAGE_ENDPOINT = DEFAULT_SSE_ENDPOINT + "/message";
	public static final String ENDPOINT_EVENT_TYPE = "endpoint";
	private static final Logger log = LoggerFactory.getLogger(SseTransport.class);
	private final McpServer mcpServer;
	private final String sseEndpoint;
	private final String messageEndpoint;
	private final SessionManager sessionManager;

	public SseTransport(McpServer mcpServer) {
		this(mcpServer, DEFAULT_SSE_ENDPOINT, DEFAULT_MESSAGE_ENDPOINT);
	}

	public SseTransport(McpServer mcpServer, String sseEndpoint, String messageEndpoint) {
		this.mcpServer = mcpServer;
		this.sseEndpoint = StrUtil.isBlank(sseEndpoint) ? DEFAULT_SSE_ENDPOINT : sseEndpoint;
		this.messageEndpoint = StrUtil.isBlank(messageEndpoint) ? DEFAULT_MESSAGE_ENDPOINT : messageEndpoint;
		this.sessionManager = new SessionManager("sse", mcpServer);
	}

	@Override
	public HttpResponse handle(HttpRequest request) {
		RequestLine requestLine = request.getRequestLine();
		String path = requestLine.getPath();
		if (sseEndpoint.equals(path) || (sseEndpoint + "/").equals(path)) {
			return handleSseConnection(request);
		} else if (path.equals(messageEndpoint) || path.startsWith(messageEndpoint + "?")) {
			return handleMessage(request);
		}
		HttpResponse resp = new HttpResponse(request);
		resp.setStatus(HttpResponseStatus.C404);
		return resp;
	}

	@Override
	public String getType() {
		return TRANSPORT_TYPE;
	}

	@Override
	public McpTransport sessionTimeout(long timeoutMs) {
		sessionManager.sessionTimeout(timeoutMs);
		return this;
	}

	@Override
	public long getSessionTimeout() {
		return sessionManager.getSessionTimeout();
	}

	@Override
	public McpTransport heartbeatInterval(long intervalMs) {
		sessionManager.heartbeatInterval(intervalMs);
		return this;
	}

	@Override
	public long getHeartbeatInterval() {
		return sessionManager.getHeartbeatInterval();
	}

	@Override
	public void sendHeartbeat() {
		sessionManager.sendHeartbeat();
	}

	@Override
	public void close() {
		sessionManager.close();
	}

	/**
	 * 处理 SSE 连接（GET /sse）。
	 */
	public HttpResponse handleSseConnection(HttpRequest request) {
		HttpResponse httpResponse = new HttpResponse(request);
		HttpStream stream = httpResponse.startSse(request);
		httpResponse.setPacketListener((context, packet, isSentSuccess) -> {
			if (isSentSuccess) {
				String sessionId = StrUtil.getNanoId();
				sessionManager.createSession(sessionId, stream);
				// 立即下发 endpoint 事件，告知 client message 端点 URL
				stream.send(ENDPOINT_EVENT_TYPE, messageEndpoint + "?sessionId=" + sessionId);
				log.debug("SSE session created: {}", sessionId);
			}
		});
		return httpResponse;
	}

	/**
	 * 处理消息（POST /sse/message?sessionId=xxx）。
	 */
	public HttpResponse handleMessage(HttpRequest request) {
		String sessionId = request.getParam("sessionId");
		HttpResponse response = new HttpResponse(request);

		if (StrUtil.isBlank(sessionId)) {
			return writeJsonRpcError(response, request, null,
				JsonRpcErrorCodes.INVALID_PARAMS, "Session ID missing in message endpoint");
		}

		McpServerSession session = sessionManager.get(sessionId);
		if (session == null) {
			log.error("Session is null sessionId:{}", sessionId);
			return writeJsonRpcError(response, request, null,
				JsonRpcErrorCodes.INVALID_PARAMS, "Unknown session: " + sessionId);
		}

		byte[] body = request.getBody();
		if (body == null || body.length == 0) {
			return writeJsonRpcError(response, request, session,
				JsonRpcErrorCodes.INVALID_REQUEST, "Empty request body");
		}

		JsonRpcMessage jsonRpcMessage;
		try {
			jsonRpcMessage = McpServer.deserializeJsonRpcMessage(body);
		} catch (Exception e) {
			log.warn("Failed to parse JSON-RPC message: {}", e.getMessage());
			return writeJsonRpcError(response, request, session,
				JsonRpcErrorCodes.PARSE_ERROR, "Parse error: " + e.getMessage());
		}

		if (jsonRpcMessage instanceof JsonRpcRequest) {
			JsonRpcResponse rpcResponse = mcpServer.handleIncomingRequest(session, (JsonRpcRequest) jsonRpcMessage);
			session.sendMessage(rpcResponse);
		} else if (jsonRpcMessage instanceof JsonRpcNotification) {
			handleNotification(session, (JsonRpcNotification) jsonRpcMessage);
		} else {
			log.debug("Discarding non-request message on SSE message endpoint: {}", jsonRpcMessage);
		}
		return response;
	}

	private void handleNotification(McpServerSession session, JsonRpcNotification notification) {
		String method = notification.getMethod();
		if (StrUtil.isBlank(method)) {
			return;
		}
		switch (method) {
			case "notifications/initialized":
				log.debug("Session {} initialized", session.getSessionId());
				break;
			case "notifications/cancelled":
				log.debug("Session {} cancelled: {}", session.getSessionId(), notification.getParams());
				break;
			case "notifications/roots/list_changed":
				log.debug("Session {} roots changed", session.getSessionId());
				break;
			default:
				log.debug("Unhandled notification: {}", method);
		}
	}

	private HttpResponse writeJsonRpcError(HttpResponse response, HttpRequest request, McpServerSession session,
	                                      int code, String message) {
		Object id = extractRequestId(request);
		JsonRpcResponse errorResp = buildError(id, code, message);
		if (session != null) {
			session.sendMessage(errorResp);
		}
		return response;
	}

	private static Object extractRequestId(HttpRequest request) {
		if (request == null) {
			return null;
		}
		byte[] body = request.getBody();
		if (body == null || body.length == 0) {
			return null;
		}
		try {
			Map<String, Object> map = JsonUtil.readMap(body);
			return map.get("id");
		} catch (Exception ignore) {
			return null;
		}
	}

	private static JsonRpcResponse buildError(Object id, int code, String message) {
		JsonRpcResponse resp = new JsonRpcResponse();
		resp.setJsonrpc("2.0");
		resp.setId(id);
		net.dreamlu.mica.net.http.jsonrpc.JsonRpcError error = new net.dreamlu.mica.net.http.jsonrpc.JsonRpcError();
		error.setCode(code);
		error.setMessage(message);
		resp.setError(error);
		return resp;
	}

	public String getSseEndpoint() {
		return sseEndpoint;
	}

	public String getMessageEndpoint() {
		return messageEndpoint;
	}

	public int getSessionCount() {
		return sessionManager.size();
	}
}
