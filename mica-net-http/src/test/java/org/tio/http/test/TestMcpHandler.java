package org.tio.http.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.http.common.*;
import org.tio.http.common.handler.HttpRequestHandler;
import org.tio.http.common.jsonrpc.JsonRpcMessage;
import org.tio.http.common.jsonrpc.JsonRpcNotification;
import org.tio.http.common.jsonrpc.JsonRpcRequest;
import org.tio.http.common.jsonrpc.JsonRpcResponse;
import org.tio.http.common.mcp.schema.*;
import org.tio.http.common.sse.SseEmitter;
import org.tio.utils.hutool.StrUtil;
import org.tio.utils.json.JsonUtil;
import org.tio.utils.thread.ThreadUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TestMcpHandler implements HttpRequestHandler {
	private static final Logger logger = LoggerFactory.getLogger(TestMcpHandler.class);
	private static ConcurrentMap<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

	@Override
	public HttpResponse handler(HttpRequest request) throws Exception {
		RequestLine requestLine = request.getRequestLine();
		String path = requestLine.getPath();
		System.out.println(path);
		HttpResponse httpResponse = new HttpResponse(request);
		if ("/sse".equals(path)) {
			// 跨域支持
			httpResponse.addHeader(HeaderName.Access_Control_Allow_Origin, HeaderValue.from("*"));
			SseEmitter emitter = SseEmitter.getEmitter(request, httpResponse);
			String uuid = StrUtil.getNanoId();
			sseEmitters.put(uuid, emitter);
			new Thread(() -> {
				ThreadUtils.sleep(1000);
				// 发送 sse
				emitter.send("endpoint", "/sse/message?sessionId=" + uuid);
			}).start();
			return httpResponse;
		} else if ("/sse/message".equals(path)) {
			String sessionId = request.getParam("sessionId");
			SseEmitter emitter = sseEmitters.get(sessionId);
			JsonRpcMessage jsonRpcMessage = deserializeJsonRpcMessage(request.getBody());
			System.out.println(jsonRpcMessage);
			if (jsonRpcMessage instanceof JsonRpcRequest) {
				JsonRpcResponse rpcResponse = handleIncomingRequest((JsonRpcRequest) jsonRpcMessage);
				String jsonString = JsonUtil.toJsonString(rpcResponse);
				System.out.println(jsonString);
				emitter.send("message", jsonString);
			} else if (jsonRpcMessage instanceof JsonRpcNotification) {
				JsonRpcNotification notification = (JsonRpcNotification) jsonRpcMessage;
				System.out.println(notification);
			}
			return httpResponse;
		}
		Collection<Cookie> cookies = request.getCookies();
		for (Cookie cookie : cookies) {
			System.out.println(cookie.getName() + "\t" + cookie.getValue());
			httpResponse.addCookie(cookie);
		}
		System.out.println(request.getCookieMap());

		httpResponse.setStatus(HttpResponseStatus.C200);
		byte[] body = request.getBody();
		if (body != null) {
			System.out.println(new String(body));
		}

		httpResponse.setBody("hello".getBytes(StandardCharsets.UTF_8));
		return httpResponse;
	}

	@Override
	public HttpResponse resp404(HttpRequest request, RequestLine requestLine) throws Exception {
		HttpResponse httpResponse = new HttpResponse(request);
		httpResponse.setStatus(HttpResponseStatus.C404);
		return httpResponse;
	}

	@Override
	public HttpResponse resp500(HttpRequest request, RequestLine requestLine, Throwable throwable) throws Exception {
		HttpResponse httpResponse = new HttpResponse(request);
		httpResponse.setStatus(HttpResponseStatus.C500);
		httpResponse.setBody(throwable.getMessage().getBytes(StandardCharsets.UTF_8));
		return httpResponse;
	}

	public static JsonRpcMessage deserializeJsonRpcMessage(byte[] requestBody) {
		Map<String, Object> map = JsonUtil.readValue(requestBody, Map.class);

		String jsonText = new String(requestBody);
		logger.debug("Received JSON message: {}", jsonText);

		// Determine message type based on specific JSON structure
		if (map.containsKey("method") && map.containsKey("id")) {
			return JsonUtil.convertValue(map, JsonRpcRequest.class);
		}
		else if (map.containsKey("method") && !map.containsKey("id")) {
			return JsonUtil.convertValue(map, JsonRpcNotification.class);
		}
		else if (map.containsKey("result") || map.containsKey("error")) {
			return JsonUtil.convertValue(map, JsonRpcResponse.class);
		}
		throw new IllegalArgumentException("Cannot deserialize JsonRpcMessage: " + jsonText);
	}

	/**
	 * Handles an incoming JSON-RPC request by routing it to the appropriate handler.
	 * @param request The incoming JSON-RPC request
	 * @return A Mono containing the JSON-RPC response
	 */
	private JsonRpcResponse handleIncomingRequest(JsonRpcRequest request) {
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
			jsonRpcResponse.setResult(toolResult);
			return jsonRpcResponse;
		}
		return null;
	}

	public static void main(String[] args) {
		McpCallToolResult toolResult = new McpCallToolResult();

		Map<String, Object> json = new HashMap<>();
		json.put("status", "123123");

		McpTextContent content = new McpTextContent(JsonUtil.toJsonString(json));

		toolResult.setContent(Collections.singletonList(content));

		System.out.println(JsonUtil.toJsonString(toolResult));
	}
}
