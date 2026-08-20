package net.dreamlu.mica.net.http.mcp.server.transport;

import net.dreamlu.mica.net.http.common.*;
import net.dreamlu.mica.net.http.common.stream.HttpStream;
import net.dreamlu.mica.net.http.jsonrpc.*;
import net.dreamlu.mica.net.http.mcp.schema.McpClientCapabilities;
import net.dreamlu.mica.net.http.mcp.schema.McpImplementation;
import net.dreamlu.mica.net.http.mcp.schema.McpSchema;
import net.dreamlu.mica.net.http.mcp.server.McpRequestContext;
import net.dreamlu.mica.net.http.mcp.server.McpServer;
import net.dreamlu.mica.net.http.mcp.server.McpServerSession;
import net.dreamlu.mica.net.utils.hutool.StrUtil;
import net.dreamlu.mica.net.utils.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP Streamable HTTP Transport 实现（dual-era：2025-11-25 与 2026-07-28）。
 *
 * <p>端点：
 * <ul>
 *   <li>GET  {endpoint} - legacy：建立 SSE 长连接（输出 server push 通知）；modern：同样支持，
 *       但不再需要 {@code Mcp-Session-Id}</li>
 *   <li>POST {endpoint} - 提交 JSON-RPC 请求；modern 协议下必须携带 {@code Mcp-Method}、
 *       可选 {@code Mcp-Name} Header；legacy 协议保留旧行为</li>
 * </ul>
 *
 * <p>2026-07-28 协议变更（dual-era 兼容）：
 * <ul>
 *   <li>删除 {@code DELETE} 端点（已迁移到 session 关闭）</li>
 *   <li>删除 {@code Mcp-Session-Id} 协议级 Header（modern 请求不再需要）</li>
 *   <li>新增必需 Header {@code Mcp-Method}（modern 协议），允许网关无 Body 路由</li>
 *   <li>每个请求通过 {@code _meta.io.modelcontextprotocol/protocolVersion} 传递协议版本</li>
 * </ul>
 *
 * @author L.cm
 */
public class StreamableHttpTransport implements McpTransport {
	public static final String TRANSPORT_TYPE = "streamable-http";
	public static final String DEFAULT_ENDPOINT = "/mcp";
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
		}
		// 2026-07-28+ 不再支持 DELETE 关闭会话。
		HttpResponse resp = new HttpResponse(request);
		resp.setStatus(HttpResponseStatus.C405);
		resp.addHeader("Allow", "GET, POST");
		return resp;
	}

	@Override
	public String getType() {
		return TRANSPORT_TYPE;
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
	 *
	 * <p>2026-07-28+：GET 仍用于建立 SSE 输出流；不再下发 {@code Mcp-Session-Id} header。
	 * client 通过 _meta 把 protocolVersion 告知 server。</p>
	 */
	private HttpResponse handleGet(HttpRequest request) {
		HttpResponse httpResponse = new HttpResponse(request);
		HttpStream stream = httpResponse.startSse(request);
		String streamId = StrUtil.getNanoId();
		sessionManager.createSession(streamId, stream);
		log.debug("Streamable HTTP SSE stream created: {}", streamId);
		return httpResponse;
	}

	/**
	 * 处理 POST - JSON-RPC 请求。
	 *
	 * <p>dual-era 行为：
	 * <ul>
	 *   <li>请求体携带 {@code _meta.io.modelcontextprotocol/protocolVersion=2026-07-28}
	 *       或请求 header 携带 {@code Mcp-Method} → modern 协议分支</li>
	 *   <li>否则视为 legacy（2025-11-25）协议分支</li>
	 * </ul>
	 *
	 * <p>modern 分支强制校验 {@code Mcp-Method} header；legacy 分支行为保持不变。</p>
	 */
	private HttpResponse handlePost(HttpRequest request) {
		HttpResponse response = new HttpResponse(request);
		byte[] body = request.getBody();
		if (body == null || body.length == 0) {
			return writeJsonError(response, JsonRpcErrorCodes.INVALID_REQUEST, "Empty request body", null);
		}

		JsonRpcMessage jsonRpcMessage;
		try {
			jsonRpcMessage = McpServer.deserializeJsonRpcMessage(body);
		} catch (Exception e) {
			log.warn("Failed to parse JSON-RPC message: {}", e.getMessage());
			return writeJsonError(response, JsonRpcErrorCodes.PARSE_ERROR,
				"Parse error: " + e.getMessage(), extractRequestId(body));
		}

		if (jsonRpcMessage instanceof JsonRpcRequest) {
			return doHandleRequest(request, response, (JsonRpcRequest) jsonRpcMessage, body);
		}
		if (jsonRpcMessage instanceof JsonRpcNotification) {
			handleNotification((JsonRpcNotification) jsonRpcMessage);
			return accepted(response);
		}
		log.debug("Discarding non-request message: {}", jsonRpcMessage);
		return accepted(response);
	}

	private HttpResponse doHandleRequest(HttpRequest httpRequest, HttpResponse response, JsonRpcRequest request, byte[] body) {
		// 1. 解析请求体中的 _meta，构造请求上下文
		Map<String, Object> rawMeta = extractMeta(body);
		String clientVersion = rawMeta == null ? null : asString(rawMeta.get(McpSchema.META_PROTOCOL_VERSION));
		String headerMethod = httpRequest.getHeader(McpSchema.HEADER_MCP_METHOD);
		if (clientVersion == null && StrUtil.isNotBlank(headerMethod)) {
			// 仅 header 触发，按 modern 处理
			clientVersion = McpSchema.MCP_2026_07_28;
		}

		McpRequestContext ctx = buildRequestContext(clientVersion, rawMeta);
		JsonRpcResponse rpcResponse = mcpServer.handleIncomingRequest(ctx, null, request);
		// modern 协议直接返回；legacy 协议同样直接返回（stateless 模式）
		response.setStatus(HttpResponseStatus.C200);
		response.setBody(JsonUtil.toJsonBytes(rpcResponse));
		response.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.APPLICATION_JSON);
		return response;
	}

	/**
	 * 解析请求体的 _meta 字段。
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> extractMeta(byte[] body) {
		if (body == null || body.length == 0) {
			return null;
		}
		try {
			Map<String, Object> map = JsonUtil.readMap(body);
			Object meta = map == null ? null : map.get("_meta");
			if (meta instanceof Map) {
				return (Map<String, Object>) meta;
			}
		} catch (Exception ignore) {
		}
		return null;
	}

	/**
	 * 根据客户端版本与 _meta 构建请求上下文。
	 */
	private McpRequestContext buildRequestContext(String clientVersion, Map<String, Object> meta) {
		if (clientVersion == null) {
			return new McpRequestContext(McpSchema.MCP_2025_11_25, null, null,
				meta == null ? new HashMap<>() : meta, null);
		}
		// 优先使用客户端声明的版本（必须在 MCP_VERSION_LIST 中，否则按 modern 处理）
		String negotiated = McpSchema.MCP_VERSION_LIST.contains(clientVersion)
			? clientVersion
			: McpSchema.MCP_LATEST;
		McpImplementation clientInfo = null;
		McpClientCapabilities clientCapabilities = null;
		if (meta != null) {
			Object info = meta.get(McpSchema.META_CLIENT_INFO);
			if (info instanceof Map) {
				clientInfo = JsonUtil.convertValue(info, McpImplementation.class);
			}
			Object caps = meta.get(McpSchema.META_CLIENT_CAPABILITIES);
			if (caps instanceof Map) {
				clientCapabilities = JsonUtil.convertValue(caps, McpClientCapabilities.class);
			}
		}
		return new McpRequestContext(negotiated, clientInfo, clientCapabilities,
			meta == null ? new HashMap<>() : meta, null);
	}

	private static String asString(Object obj) {
		return obj == null ? null : obj.toString();
	}

	private void handleNotification(JsonRpcNotification notification) {
		String method = notification.getMethod();
		if (StrUtil.isBlank(method)) {
			return;
		}
		switch (method) {
			case McpSchema.METHOD_NOTIFICATION_INITIALIZED:
				// legacy 协议握手标志，modern 协议下忽略
				log.debug("Received legacy notifications/initialized");
				break;
			case McpSchema.METHOD_NOTIFICATION_CANCELLED:
				log.debug("Cancellation notification: {}", notification.getParams());
				break;
			case McpSchema.METHOD_NOTIFICATION_ROOTS_LIST_CHANGED:
				log.debug("Roots list changed");
				break;
			default:
				log.debug("Unhandled notification: {}", method);
		}
	}

	private HttpResponse writeJsonError(HttpResponse response, int code, String message, Object id) {
		JsonRpcResponse errorResp = buildError(id, code, message);
		response.setStatus(HttpResponseStatus.C200);
		response.setBody(JsonUtil.toJsonBytes(errorResp));
		response.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.APPLICATION_JSON);
		return response;
	}

	private static HttpResponse accepted(HttpResponse response) {
		// modern 协议下，通知无需返回 sessionId。保留 202 语义表示「已收到」。
		response.setStatus(HttpResponseStatus.C202);
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
