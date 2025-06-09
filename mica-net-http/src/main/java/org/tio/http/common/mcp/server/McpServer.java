package org.tio.http.common.mcp.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.http.common.HttpResponseStatus;
import org.tio.http.common.jsonrpc.JsonRpcMessage;
import org.tio.http.common.jsonrpc.JsonRpcNotification;
import org.tio.http.common.jsonrpc.JsonRpcRequest;
import org.tio.http.common.jsonrpc.JsonRpcResponse;
import org.tio.http.common.mcp.schema.*;
import org.tio.http.common.sse.SseEmitter;
import org.tio.utils.hutool.StrUtil;
import org.tio.utils.json.JsonUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class McpServer {
	private static final Logger log = LoggerFactory.getLogger(McpServer.class);
	/**
	 * Event type for sending the message endpoint URI to clients.
	 */
	public static final String ENDPOINT_EVENT_TYPE = "endpoint";

	/**
	 * Default SSE endpoint path as specified by the MCP transport specification.
	 */
	public static final String DEFAULT_SSE_ENDPOINT = "/sse";

	/**
	 * Default message endpoint path as specified by the MCP transport specification.
	 */
	public static final String DEFAULT_MESSAGE_ENDPOINT = DEFAULT_SSE_ENDPOINT + "/message";

	/**
	 * Map of active client sessions, keyed by session ID.
	 */
	private final ConcurrentHashMap<String, McpServerSession> sessions = new ConcurrentHashMap<>();

	private final String sseEndpoint;
	private final String messageEndpoint;

	public McpServer() {
		this(DEFAULT_SSE_ENDPOINT, DEFAULT_MESSAGE_ENDPOINT);
	}

	public McpServer(String sseEndpoint, String messageEndpoint) {
		this.sseEndpoint = Objects.requireNonNull(sseEndpoint, "SSE endpoint must not be null");
		this.messageEndpoint = Objects.requireNonNull(messageEndpoint, "Message endpoint must not be null");
	}

	/**
	 * sse endpoint
	 *
	 * @param request HttpRequest
	 * @return HttpResponse
	 */
	public HttpResponse sseEndpoint(HttpRequest request) {
		HttpResponse httpResponse = new HttpResponse(request);
		// 保存 session
		SseEmitter emitter = SseEmitter.getEmitter(request, httpResponse);
		String sessionId = StrUtil.getNanoId();
		sessions.put(sessionId, new McpServerSession(sessionId, emitter));
		// 响应包发送后，再发送 sse 回包
		httpResponse.setPacketListener((context, packet, isSentSuccess) -> {
			if (isSentSuccess) {
				emitter.send(ENDPOINT_EVENT_TYPE,  messageEndpoint + "?sessionId=" + sessionId);
			}
		});
		return httpResponse;
	}

	/**
	 * sse message endpoint
	 *
	 * @param request HttpRequest
	 * @return HttpResponse
	 */
	public HttpResponse sseMessageEndpoint(HttpRequest request) {
		// session id
		String sessionId = request.getParam("sessionId");
		HttpResponse response = new HttpResponse(request);
		if (StrUtil.isBlank(sessionId)) {
			response.setStatus(HttpResponseStatus.C404);
			log.error("Session ID missing in message endpoint");
			return response;
		}
		McpServerSession session = sessions.get(sessionId);
		JsonRpcMessage jsonRpcMessage = deserializeJsonRpcMessage(request.getBody());
		if (jsonRpcMessage instanceof JsonRpcRequest) {
			JsonRpcResponse rpcResponse = handleIncomingRequest((JsonRpcRequest) jsonRpcMessage);
			session.sendMessage(rpcResponse);
		} else if (jsonRpcMessage instanceof JsonRpcNotification) {
			JsonRpcNotification notification = (JsonRpcNotification) jsonRpcMessage;
			System.out.println(notification);
		}
		return response;
	}

	/**
	 * 发送心跳
	 */
	public void sendHeartbeat() {
		for (McpServerSession session : sessions.values()) {
			session.sendHeartbeat();
		}
	}

	public String getMessageEndpoint() {
		return messageEndpoint;
	}

	public String getSseEndpoint() {
		return sseEndpoint;
	}

	private static JsonRpcMessage deserializeJsonRpcMessage(byte[] requestBody) {
		Map<String, Object> map = JsonUtil.readValue(requestBody, Map.class);

		String jsonText = new String(requestBody);
		log.debug("Received JSON message: {}", jsonText);

		// Determine message type based on specific JSON structure
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

	/**
	 * Handles an incoming JSON-RPC request by routing it to the appropriate handler.
	 * @param request The incoming JSON-RPC request
	 * @return A Mono containing the JSON-RPC response
	 */
	private static JsonRpcResponse handleIncomingRequest(JsonRpcRequest request) {
		String method = request.getMethod();
		if (McpSchema.METHOD_INITIALIZE.equals(method)) {
			Object params = request.getParams();
			McpInitializeRequest initializeRequest = JsonUtil.convertValue(params, McpInitializeRequest.class);
//			this.init(initializeRequest.getCapabilities(), initializeRequest.getClientInfo());

			McpInitializeResult result = new McpInitializeResult();
			result.setProtocolVersion(initializeRequest.getProtocolVersion());
			McpServerCapabilities serverCapabilities = new McpServerCapabilities();
			McpLoggingCapabilities logging = new McpLoggingCapabilities();
			serverCapabilities.setLogging(logging);
			McpPromptCapabilities prompts = new McpPromptCapabilities();
			prompts.setListChanged(false);
			serverCapabilities.setPrompts(prompts);
			McpResourceCapabilities resources = new McpResourceCapabilities();
			resources.setListChanged(false);
			resources.setSubscribe(false);
			serverCapabilities.setResources(resources);
			McpToolCapabilities tools = new McpToolCapabilities();
			tools.setListChanged(true);
			serverCapabilities.setTools(tools);
			result.setCapabilities(serverCapabilities);

			McpImplementation implementation = new McpImplementation();
			implementation.setName("McpServerTool");
			implementation.setVersion(McpSchema.LATEST_PROTOCOL_VERSION);
			result.setServerInfo(implementation);

			JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
			jsonRpcResponse.setJsonrpc(McpSchema.JSONRPC_VERSION);
			jsonRpcResponse.setId(request.getId());
			jsonRpcResponse.setResult(result);
			return jsonRpcResponse;
		} else if (McpSchema.METHOD_PING.equals(method)) {
			JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
			jsonRpcResponse.setJsonrpc(McpSchema.JSONRPC_VERSION);
			jsonRpcResponse.setId(request.getId());
			jsonRpcResponse.setResult(Collections.emptyMap());
			return jsonRpcResponse;
		} else if (McpSchema.METHOD_TOOLS_LIST.equals(method)) {
			JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
			jsonRpcResponse.setJsonrpc(McpSchema.JSONRPC_VERSION);
			jsonRpcResponse.setId(request.getId());

			McpTool mcpTool = new McpTool();
			mcpTool.setName("mqttStatus");
			mcpTool.setDescription("获取 mqtt 状态");
			mcpTool.setReturnDirect(true);

			McpJsonSchema jsonSchemaIn = new McpJsonSchema();
			jsonSchemaIn.setType("object");
			jsonSchemaIn.setProperties(new HashMap<>());
			jsonSchemaIn.setRequired(new ArrayList<>());
			mcpTool.setInputSchema(jsonSchemaIn);

			McpJsonSchema jsonSchemaOut = new McpJsonSchema();
			jsonSchemaOut.setType("object");
			Map<String, Object> properties = new HashMap<>();

			Map<String, Object> status = new HashMap<>();
			status.put("type", "string");
			status.put("description", "mqtt status");

			properties.put("status", status);
			jsonSchemaOut.setProperties(properties);
			jsonSchemaOut.setRequired(Collections.singletonList("status"));
			mcpTool.setOutputSchema(jsonSchemaOut);

			McpListToolsResult toolsResult = new McpListToolsResult();
			toolsResult.setTools(Collections.singletonList(mcpTool));
			jsonRpcResponse.setResult(toolsResult);
			return jsonRpcResponse;
		} else if (McpSchema.METHOD_TOOLS_CALL.equals(method)) {
			JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
			jsonRpcResponse.setJsonrpc(McpSchema.JSONRPC_VERSION);
			jsonRpcResponse.setId(request.getId());

			McpCallToolResult toolResult = new McpCallToolResult();

			Map<String, Object> json = new HashMap<>();
			json.put("status", "123123");

			McpTextContent content = new McpTextContent(JsonUtil.toJsonString(json));

			toolResult.setContent(Collections.singletonList(content));
			toolResult.setStructuredContent(json);

			jsonRpcResponse.setResult(toolResult);
			return jsonRpcResponse;
		}
		return null;
	}
}
