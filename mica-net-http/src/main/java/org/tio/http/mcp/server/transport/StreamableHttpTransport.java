package org.tio.http.mcp.server.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.http.common.*;
import org.tio.http.common.stream.HttpStream;
import org.tio.http.jsonrpc.JsonRpcRequest;
import org.tio.http.jsonrpc.JsonRpcResponse;
import org.tio.http.mcp.server.McpServer;
import org.tio.http.mcp.server.McpServerSession;
import org.tio.utils.hutool.StrUtil;
import org.tio.utils.json.JsonUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Streamable HTTP Transport 实现
 * <p>
 * 单一端点同时支持 GET (SSE) 和 POST (JSON-RPC) 请求
 *
 * @author L.cm
 */
public class StreamableHttpTransport implements McpTransport {
	private static final Logger log = LoggerFactory.getLogger(StreamableHttpTransport.class);

	public static final String TRANSPORT_TYPE = "streamable-http";
	public static final String DEFAULT_ENDPOINT = "/mcp";
	public static final String MESSAGE_EVENT_TYPE = "message";
	public static final String ENDPOINT_EVENT_TYPE = "endpoint";

	private final McpServer mcpServer;
	private final String endpoint;
	/**
	 * Streamable HTTP 使用 session 状态共享
	 */
	private final Map<String, StreamableSession> sessions = new ConcurrentHashMap<>();

	public StreamableHttpTransport(McpServer mcpServer) {
		this(mcpServer, DEFAULT_ENDPOINT);
	}

	public StreamableHttpTransport(McpServer mcpServer, String endpoint) {
		this.mcpServer = mcpServer;
		this.endpoint = StrUtil.isBlank(endpoint) ? DEFAULT_ENDPOINT : endpoint;
	}

	@Override
	public HttpResponse handle(HttpRequest request) {
		RequestLine requestLine = request.getRequestLine();
		String path = requestLine.getPath();
		if (!path.equals(endpoint) && !path.equals(endpoint + "/")) {
			// 不是我们的端点，返回404让其他handler处理
			HttpResponse resp = new HttpResponse(request);
			resp.setStatus(HttpResponseStatus.C404);
			return resp;
		}
		Method method = requestLine.getMethod();
		String accept = request.getHeader("Accept");
		if (Method.GET.equals(method) && isEventStreamAccept(accept)) {
			return handleSseConnection(request);
		} else if (Method.POST.equals(method)) {
			return handleJsonRpcRequest(request);
		} else {
			HttpResponse resp = new HttpResponse(request);
			resp.setStatus(HttpResponseStatus.C405);
			resp.setBody(("Method " + method + " not allowed for this endpoint").getBytes());
			return resp;
		}
	}

	@Override
	public String getType() {
		return TRANSPORT_TYPE;
	}

	/**
	 * 检查 Accept header 是否为 text/event-stream
	 */
	private boolean isEventStreamAccept(String accept) {
		if (accept == null) {
			return false;
		}
		// Accept 可能包含多个类型，如 "text/event-stream, application/json"
		String[] types = accept.split(",");
		for (String type : types) {
			if (type.trim().equals("text/event-stream")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 处理 SSE 连接（GET /mcp）
	 */
	private HttpResponse handleSseConnection(HttpRequest request) {
		HttpResponse httpResponse = new HttpResponse(request);
		HttpStream stream = httpResponse.startSse(request);

		httpResponse.setPacketListener((context, packet, isSentSuccess) -> {
			if (isSentSuccess) {
				String sessionId = StrUtil.getNanoId();
				StreamableSession session = new StreamableSession(sessionId, stream);
				sessions.put(sessionId, session);
				// 发送 endpoint 事件，包含后续 POST 消息的 URL
				stream.send(ENDPOINT_EVENT_TYPE, endpoint + "?sessionId=" + sessionId);
			}
		});
		return httpResponse;
	}

	/**
	 * 处理 JSON-RPC 请求（POST /mcp）
	 */
	private HttpResponse handleJsonRpcRequest(HttpRequest request) {
		String sessionId = request.getParam("sessionId");
		StreamableSession session = sessionId != null ? sessions.get(sessionId) : null;

		// 如果没有 session，创建一个临时的（用于不支持 cookie 的客户端）
		if (session == null) {
			// 创建一个无 SSE 的 session 用于处理请求
			session = new StreamableSession(StrUtil.getNanoId(), null);
		}

		// 处理 JSON-RPC 消息
		byte[] body = request.getBody();
		if (body == null || body.length == 0) {
			HttpResponse resp = new HttpResponse(request);
			resp.setStatus(HttpResponseStatus.C400);
			resp.setBody("Request body is empty".getBytes());
			return resp;
		}

		try {
			Map<String, Object> jsonMap = JsonUtil.readValue(body, Map.class);
			String method = (String) jsonMap.get("method");

			if (method == null) {
				// 可能是响应或错误，不是请求
				HttpResponse resp = new HttpResponse(request);
				resp.setStatus(HttpResponseStatus.C400);
				resp.setBody("Missing method in request".getBytes());
				return resp;
			}

			// 构建请求对象
			JsonRpcRequest rpcRequest = JsonUtil.convertValue(jsonMap, JsonRpcRequest.class);

			// 使用虚拟 session 处理请求
			McpServerSession tempSession = new McpServerSession(
				session.getSessionId(),
				new DummyHttpStream()
			);

			JsonRpcResponse rpcResponse = mcpServer.handleIncomingRequest(tempSession, rpcRequest);

			// 如果 session 有 SSE 流，发送响应
			if (session.getStream() != null) {
				session.getStream().send(MESSAGE_EVENT_TYPE, JsonUtil.toJsonString(rpcResponse));
			}

			// 返回 HTTP 响应
			HttpResponse resp = new HttpResponse(request);
			resp.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.APPLICATION_JSON);
			resp.setBody(JsonUtil.toJsonString(rpcResponse).getBytes());
			return resp;

		} catch (Exception e) {
			log.error("Error handling JSON-RPC request", e);
			HttpResponse resp = new HttpResponse(request);
			resp.setStatus(HttpResponseStatus.C500);
			resp.setBody(("Internal error: " + e.getMessage()).getBytes());
			return resp;
		}
	}

	/**
	 * 获取端点路径
	 */
	public String getEndpoint() {
		return endpoint;
	}

	/**
	 * 获取 session 数量
	 */
	public int getSessionCount() {
		return sessions.size();
	}

	/**
	 * Streamable Session 管理
	 */
	public static class StreamableSession {
		private final String sessionId;
		private final HttpStream stream;

		public StreamableSession(String sessionId, HttpStream stream) {
			this.sessionId = sessionId;
			this.stream = stream;
		}

		public String getSessionId() {
			return sessionId;
		}

		public HttpStream getStream() {
			return stream;
		}
	}

	/**
	 * 虚拟 HttpStream，用于无 SSE 的请求处理
	 */
	private static class DummyHttpStream extends HttpStream {
		public DummyHttpStream() {
			super(null, org.tio.http.common.stream.HttpStreamType.SSE);
		}

		@Override
		public void send(byte[] data) {
			// do nothing
		}

		@Override
		public void send(Object data) {
			// do nothing
		}

		@Override
		public void send(String event, Object data) {
			// do nothing
		}

		@Override
		public void close() {
			// do nothing
		}
	}
}
