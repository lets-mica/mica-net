package net.dreamlu.mica.net.http.mcp.server.transport;

import net.dreamlu.mica.net.http.common.*;
import net.dreamlu.mica.net.http.common.stream.HttpStream;
import net.dreamlu.mica.net.http.jsonrpc.*;
import net.dreamlu.mica.net.http.mcp.server.McpServer;
import net.dreamlu.mica.net.http.mcp.server.McpServerSession;
import net.dreamlu.mica.net.utils.hutool.StrUtil;
import net.dreamlu.mica.net.utils.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * MCP Streamable HTTP Transport 实现。
 *
 * <p>采用统一的端点：
 * <ul>
 *   <li>GET  {endpoint} - 建立 SSE 长连接（可选），用于 server push 通知</li>
 *   <li>POST {endpoint} - 提交 JSON-RPC 请求；可附带 {@code Mcp-Session-Id} 关联已建立的 SSE 流</li>
 *   <li>DELETE {endpoint} - 终止 SSE 会话</li>
 * </ul>
 *
 * @author L.cm
 */
public class StreamableHttpTransport implements McpTransport {
	public static final String TRANSPORT_TYPE = "streamable-http";
	public static final String DEFAULT_ENDPOINT = "/mcp";
	/**
	 * 约定的 session id header 名
	 */
	public static final String SESSION_HEADER = "Mcp-Session-Id";
	private static final Logger log = LoggerFactory.getLogger(StreamableHttpTransport.class);

	private final McpServer mcpServer;
	private final String endpoint;
	private final SessionManager sessionManager;

	public StreamableHttpTransport(McpServer mcpServer) {
		this(mcpServer, DEFAULT_ENDPOINT);
	}

	public StreamableHttpTransport(McpServer mcpServer, String endpoint) {
		this.mcpServer = mcpServer;
		this.endpoint = StrUtil.isBlank(endpoint) ? DEFAULT_ENDPOINT : endpoint;
		this.sessionManager = new SessionManager("streamable-http", mcpServer);
	}

	@Override
	public HttpResponse handle(HttpRequest request) {
		RequestLine requestLine = request.getRequestLine();
		String path = requestLine.getPath();
		if (!endpoint.equals(path) && !(endpoint + "/").equals(path)) {
			HttpResponse resp = new HttpResponse(request);
			resp.setStatus(HttpResponseStatus.C404);
			return resp;
		}
		Method method = requestLine.getMethod();
		if (Method.GET == method) {
			return handleGet(request);
		} else if (Method.POST == method) {
			return handlePost(request);
		} else if (Method.DELETE == method) {
			return handleDelete(request);
		}
		HttpResponse resp = new HttpResponse(request);
		resp.setStatus(HttpResponseStatus.C405);
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
	 * 处理 GET - 建立 SSE 长连接。
	 */
	private HttpResponse handleGet(HttpRequest request) {
		HttpResponse httpResponse = new HttpResponse(request);
		HttpStream stream = httpResponse.startSse(request);
		String sessionId = StrUtil.getNanoId();
		sessionManager.createSession(sessionId, stream);
		httpResponse.addHeader(SESSION_HEADER, sessionId);
		log.debug("Streamable HTTP SSE session created: {}", sessionId);
		return httpResponse;
	}

	/**
	 * 处理 POST - JSON-RPC 请求。
	 *
	 * <p>两种模式：
	 * <ul>
	 *   <li>Stateless（不携带 sessionId 或 session 不存在）：直接返回 JSON-RPC 响应</li>
	 *   <li>Stateful（携带 sessionId 且 SSE 流存在）：响应也写入 SSE 流，
	 *       HTTP 响应返回 202 Accepted</li>
	 * </ul>
	 */
	private HttpResponse handlePost(HttpRequest request) {
		HttpResponse response = new HttpResponse(request);
		String sessionId = request.getHeader(SESSION_HEADER);
		if (StrUtil.isBlank(sessionId)) {
			sessionId = request.getParam("sessionId");
		}
		McpServerSession session = StrUtil.isNotBlank(sessionId) ? sessionManager.get(sessionId) : null;

		byte[] body = request.getBody();
		if (body == null || body.length == 0) {
			return writeJsonError(response, session, JsonRpcErrorCodes.INVALID_REQUEST, "Empty request body", null);
		}

		JsonRpcMessage jsonRpcMessage;
		try {
			jsonRpcMessage = McpServer.deserializeJsonRpcMessage(body);
		} catch (Exception e) {
			log.warn("Failed to parse JSON-RPC message: {}", e.getMessage());
			return writeJsonError(response, session,
				JsonRpcErrorCodes.PARSE_ERROR, "Parse error: " + e.getMessage(), extractRequestId(body));
		}

		if (jsonRpcMessage instanceof JsonRpcRequest) {
			return doHandleRequest(response, session, (JsonRpcRequest) jsonRpcMessage);
		}
		if (jsonRpcMessage instanceof JsonRpcNotification) {
			if (session != null) {
				handleNotification(session, (JsonRpcNotification) jsonRpcMessage);
			} else {
				log.debug("Discarding notification without session: {}", jsonRpcMessage);
			}
			return accepted(response, session);
		}
		log.debug("Discarding non-request message: {}", jsonRpcMessage);
		return accepted(response, session);
	}

	private HttpResponse doHandleRequest(HttpResponse response, McpServerSession session, JsonRpcRequest request) {
		JsonRpcResponse rpcResponse = mcpServer.handleIncomingRequest(session, request);
		if (session != null && session.hasStream()) {
			session.sendMessage(rpcResponse);
			return accepted(response, session);
		}
		// stateless：直接把 JSON-RPC 响应写到 HTTP body
		response.setStatus(HttpResponseStatus.C200);
		response.setBody(JsonUtil.toJsonBytes(rpcResponse));
		response.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.APPLICATION_JSON);
		return response;
	}

	/**
	 * 处理 DELETE - 主动关闭 SSE 会话。
	 */
	private HttpResponse handleDelete(HttpRequest request) {
		HttpResponse response = new HttpResponse(request);
		String sessionId = request.getHeader(SESSION_HEADER);
		if (StrUtil.isBlank(sessionId)) {
			sessionId = request.getParam("sessionId");
		}
		if (StrUtil.isBlank(sessionId)) {
			response.setStatus(HttpResponseStatus.C400);
			return response;
		}
		McpServerSession session = sessionManager.get(sessionId);
		if (session != null) {
			sessionManager.remove(sessionId);
			response.setStatus(HttpResponseStatus.C200);
		} else {
			response.setStatus(HttpResponseStatus.C404);
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

	private HttpResponse writeJsonError(HttpResponse response, McpServerSession session,
	                                    int code, String message, Object id) {
		JsonRpcResponse errorResp = buildError(id, code, message);
		if (session != null && session.hasStream()) {
			session.sendMessage(errorResp);
			return accepted(response, session);
		}
		response.setStatus(HttpResponseStatus.C200);
		response.setBody(JsonUtil.toJsonBytes(errorResp));
		response.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.APPLICATION_JSON);
		return response;
	}

	private static HttpResponse accepted(HttpResponse response, McpServerSession session) {
		response.setStatus(HttpResponseStatus.C202);
		if (session != null) {
			response.addHeader(SESSION_HEADER, session.getSessionId());
		}
		return response;
	}

	private static Object extractRequestId(byte[] body) {
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
		JsonRpcError error = new JsonRpcError();
		error.setCode(code);
		error.setMessage(message);
		resp.setError(error);
		return resp;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public int getSessionCount() {
		return sessionManager.size();
	}
}
