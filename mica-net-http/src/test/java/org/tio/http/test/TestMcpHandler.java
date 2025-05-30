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
import java.util.Collection;
import java.util.Map;

public class TestMcpHandler implements HttpRequestHandler {
	private static final Logger logger = LoggerFactory.getLogger(TestMcpHandler.class);

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
			new Thread(() -> {
				ThreadUtils.sleep(1000);
				// 发送 sse
				String uuid = StrUtil.getNanoId();
				emitter.send("endpoint", "/sse/message?sessionId=" + uuid);
			}).start();
			return httpResponse;
		} else if ("/sse/message".equals(path)) {
			JsonRpcMessage jsonRpcMessage = deserializeJsonRpcMessage(request.getBody());
			System.out.println(jsonRpcMessage);
			JsonRpcResponse rpcResponse = handleIncomingRequest((JsonRpcRequest) jsonRpcMessage);
			httpResponse.setBody(JsonUtil.toJsonBytes(rpcResponse));
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
		if (McpSchema.METHOD_INITIALIZE.equals(request.getMethod())) {
			Object params = request.getParams();
			McpInitializeRequest initializeRequest = JsonUtil.convertValue(params, McpInitializeRequest.class);
//			this.init(initializeRequest.getCapabilities(), initializeRequest.getClientInfo());

			McpInitializeResult result = new McpInitializeResult();
			result.setProtocolVersion("1.0.0");
			McpServerCapabilities serverCapabilities = new McpServerCapabilities();
			McpLoggingCapabilities logging = new McpLoggingCapabilities();
			serverCapabilities.setLogging(logging);
			McpPromptCapabilities prompts = new McpPromptCapabilities();
			prompts.setListChanged(true);
			serverCapabilities.setPrompts(prompts);
			McpResourceCapabilities resources = new McpResourceCapabilities();
			resources.setListChanged(true);
			resources.setSubscribe(true);
			serverCapabilities.setResources(resources);
			McpToolCapabilities tools = new McpToolCapabilities();
			tools.setListChanged(true);
			serverCapabilities.setTools(tools);
			result.setCapabilities(serverCapabilities);

			McpImplementation implementation = new McpImplementation();
			implementation.setName("mcp-server");
			implementation.setVersion(McpSchema.LATEST_PROTOCOL_VERSION);
			result.setServerInfo(implementation);

			JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
			jsonRpcResponse.setJsonrpc(McpSchema.JSONRPC_VERSION);
			jsonRpcResponse.setId(request.getId());
			jsonRpcResponse.setResult(result);
			return jsonRpcResponse;
		}
		return null;
//		else {
//			// TODO handle errors for communication to this session without
//			// initialization happening first
//			var handler = this.requestHandlers.get(request.getMethod());
//			if (handler == null) {
//				MethodNotFoundError error = getMethodNotFoundError(request.getMethod());
//				return Mono.just(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), null,
//					new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.METHOD_NOT_FOUND,
//						error.getMessage(), error.getData())));
//			}
//			resultMono = this.exchangeSink.asMono().flatMap(exchange -> handler.handle(exchange, request.getParams()));
//		}
//		return resultMono
//			.map(result -> new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), result, null))
//			.onErrorResume(error -> Mono.just(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(),
//				null, new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR,
//				error.getMessage(), null)))); // TODO: add error message
//		// through the data field
	}
}
