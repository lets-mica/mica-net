package net.dreamlu.mica.net.http.mcp.server.transport;

import net.dreamlu.mica.net.http.common.HttpRequest;
import net.dreamlu.mica.net.http.common.HttpResponse;
import net.dreamlu.mica.net.http.common.HttpResponseStatus;
import net.dreamlu.mica.net.http.common.RequestLine;
import net.dreamlu.mica.net.http.common.stream.HttpStream;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP SSE Transport 实现
 * <p>
 * 采用两个端点：
 * <ul>
 *   <li>GET /sse - 建立 SSE 连接</li>
 *   <li>POST /sse/message - 发送 JSON-RPC 消息</li>
 * </ul>
 *
 * @author L.cm
 */
public class SseTransport implements McpTransport {
	private static final Logger log = LoggerFactory.getLogger(SseTransport.class);

	public static final String TRANSPORT_TYPE = "sse";
	public static final String DEFAULT_SSE_ENDPOINT = "/sse";
	public static final String DEFAULT_MESSAGE_ENDPOINT = DEFAULT_SSE_ENDPOINT + "/message";
	public static final String ENDPOINT_EVENT_TYPE = "endpoint";

	private final McpServer mcpServer;
	private final String sseEndpoint;
	private final String messageEndpoint;
	private final Map<String, McpServerSession> sessions = new ConcurrentHashMap<>();

	public SseTransport(McpServer mcpServer) {
		this(mcpServer, DEFAULT_SSE_ENDPOINT, DEFAULT_MESSAGE_ENDPOINT);
	}

	public SseTransport(McpServer mcpServer, String sseEndpoint, String messageEndpoint) {
		this.mcpServer = mcpServer;
		this.sseEndpoint = StrUtil.isBlank(sseEndpoint) ? DEFAULT_SSE_ENDPOINT : sseEndpoint;
		this.messageEndpoint = StrUtil.isBlank(messageEndpoint) ? DEFAULT_MESSAGE_ENDPOINT : messageEndpoint;
	}

	@Override
	public HttpResponse handle(HttpRequest request) {
		RequestLine requestLine = request.getRequestLine();
		String path = requestLine.getPath();

		if (sseEndpoint.equals(path)) {
			return handleSseConnection(request);
		} else if (messageEndpoint.equals(path)) {
			return handleMessage(request);
		} else {
			HttpResponse resp = new HttpResponse(request);
			resp.setStatus(HttpResponseStatus.C404);
			return resp;
		}
	}

	@Override
	public String getType() {
		return TRANSPORT_TYPE;
	}

	/**
	 * 处理 SSE 连接（GET /sse）
	 */
	public HttpResponse handleSseConnection(HttpRequest request) {
		HttpResponse httpResponse = new HttpResponse(request);
		HttpStream stream = httpResponse.startSse(request);
		httpResponse.setPacketListener((context, packet, isSentSuccess) -> {
			if (isSentSuccess) {
				String sessionId = StrUtil.getNanoId();
				sessions.put(sessionId, new McpServerSession(sessionId, stream));
				stream.send(ENDPOINT_EVENT_TYPE, messageEndpoint + "?sessionId=" + sessionId);
			}
		});
		return httpResponse;
	}

	/**
	 * 处理消息（POST /sse/message）
	 */
	public HttpResponse handleMessage(HttpRequest request) {
		String sessionId = request.getParam("sessionId");
		HttpResponse response = new HttpResponse(request);

		if (StrUtil.isBlank(sessionId)) {
			response.setStatus(HttpResponseStatus.C400);
			response.setBody("Session ID missing in message endpoint".getBytes());
			return response;
		}

		McpServerSession session = sessions.get(sessionId);
		if (session == null) {
			response.setStatus(HttpResponseStatus.C400);
			response.setBody("Session is null".getBytes());
			log.error("Session is null sessionId:{}", sessionId);
			return response;
		}

		JsonRpcMessage jsonRpcMessage = deserializeJsonRpcMessage(request.getBody());
		if (jsonRpcMessage instanceof JsonRpcRequest) {
			JsonRpcResponse rpcResponse = mcpServer.handleIncomingRequest(session, (JsonRpcRequest) jsonRpcMessage);
			session.sendMessage(rpcResponse);
		} else if (jsonRpcMessage instanceof JsonRpcNotification) {
			JsonRpcNotification notification = (JsonRpcNotification) jsonRpcMessage;
			log.info("JsonRpcNotification:{}", notification);
		}
		return response;
	}

	/**
	 * 解码消息
	 */
	private static JsonRpcMessage deserializeJsonRpcMessage(byte[] requestBody) {
		Map<String, Object> map = JsonUtil.readValue(requestBody, Map.class);
		String jsonText = new String(requestBody);
		log.debug("Received JSON message: {}", jsonText);
		if (map.containsKey("method") && map.containsKey("id")) {
			return JsonUtil.convertValue(map, JsonRpcRequest.class);
		} else if (map.containsKey("method") && !map.containsKey("id")) {
			return JsonUtil.convertValue(map, JsonRpcNotification.class);
		} else if (map.containsKey("result") || map.containsKey("error")) {
			return JsonUtil.convertValue(map, JsonRpcResponse.class);
		} else {
			throw new IllegalArgumentException("Cannot deserialize JsonRpcMessage: " + jsonText);
		}
	}

	public String getSseEndpoint() {
		return sseEndpoint;
	}

	public String getMessageEndpoint() {
		return messageEndpoint;
	}

	public int getSessionCount() {
		return sessions.size();
	}

	@Override
	public void sendHeartbeat() {
		for (McpServerSession session : sessions.values()) {
			session.sendHeartbeat();
		}
	}
}
