package net.dreamlu.mica.net.http.mcp.server;

import net.dreamlu.mica.net.http.common.HttpRequest;
import net.dreamlu.mica.net.http.common.HttpResponse;
import net.dreamlu.mica.net.http.common.HttpResponseStatus;
import net.dreamlu.mica.net.http.common.router.HttpRouter;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcError;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcErrorCodes;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcMessage;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcNotification;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcRequest;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcResponse;
import net.dreamlu.mica.net.http.mcp.schema.*;
import net.dreamlu.mica.net.http.mcp.server.transport.McpTransport;
import net.dreamlu.mica.net.http.mcp.server.transport.SseTransport;
import net.dreamlu.mica.net.http.mcp.server.transport.StreamableHttpTransport;
import net.dreamlu.mica.net.http.mcp.util.UriTemplate;
import net.dreamlu.mica.net.utils.hutool.StrUtil;
import net.dreamlu.mica.net.utils.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * mcp 服务
 *
 * <p>同时承担配置（builder）与请求分发（dispatcher）职责。
 * 通过 {@link McpRequestHandler} 注册表实现 method 分发，
 * 通过 {@link McpException} 把业务异常转换为标准 JSON-RPC error 响应。</p>
 *
 * @author L.cm
 */
public class McpServer {
	private static final Logger log = LoggerFactory.getLogger(McpServer.class);

	/**
	 * 默认的服务信息
	 */
	private static final McpImplementation DEFAULT_SERVER_INFO = new McpImplementation("mcp-server", "1.0.0");

	/**
	 * 注册的传输层列表
	 */
	private final List<McpTransport> transports = new ArrayList<>();
	/**
	 * The Model Context Protocol (MCP) allows servers to expose tools that can be
	 * invoked by language models. Tools enable models to interact with external
	 * systems, such as querying databases, calling APIs, or performing computations.
	 * Each tool is uniquely identified by a name and includes metadata describing its
	 * schema.
	 */
	private final Map<String, McpToolSpecification> tools = new LinkedHashMap<>();
	/**
	 * The Model Context Protocol (MCP) provides a standardized way for servers to
	 * expose resources to clients. Resources allow servers to share data that
	 * provides context to language models, such as files, database schemas, or
	 * application-specific information. Each resource is uniquely identified by a
	 * URI.
	 */
	private final Map<String, McpResourceSpecification> resources = new LinkedHashMap<>();
	private final Map<String, McpResourceTemplateSpecification> resourceTemplates = new LinkedHashMap<>();
	/**
	 * The Model Context Protocol (MCP) provides a standardized way for servers to
	 * expose prompt templates to clients. Prompts allow servers to provide structured
	 * messages and instructions for interacting with language models. Clients can
	 * discover available prompts, retrieve their contents, and provide arguments to
	 * customize them.
	 */
	private final Map<String, McpPromptSpecification> prompts = new LinkedHashMap<>();
	private final List<BiConsumer<McpServerSession, List<McpRoot>>> rootsChangeHandlers = new ArrayList<>();
	/**
	 * Method handler 注册表，将 JSON-RPC method 名映射到 handler。
	 */
	private final Map<String, McpRequestHandler> methodHandlers = new ConcurrentHashMap<>();
	/**
	 * 每个 session 的日志级别，per-session 存储以避免 session 间互相干扰。
	 */
	private final Map<String, McpLoggingLevel> sessionLogLevels = new ConcurrentHashMap<>();
	/**
	 * session 注册表，sessionId → session，供 server 主动推送通知使用。
	 */
	private final Map<String, McpServerSession> sessionRegistry = new ConcurrentHashMap<>();

	private McpImplementation serverInfo = DEFAULT_SERVER_INFO;
	private McpServerCapabilities serverCapabilities;

	public McpServer() {
		registerBuiltinHandlers();
	}

	// ============================================================
	//  Method handler 注册
	// ============================================================

	/**
	 * 注册所有内置 method handler。
	 */
	private void registerBuiltinHandlers() {
		methodHandlers.put(McpSchema.METHOD_INITIALIZE, this::handleInitialize);
		methodHandlers.put(McpSchema.METHOD_PING, this::handlePing);
		methodHandlers.put(McpSchema.METHOD_TOOLS_LIST, this::handleToolsList);
		methodHandlers.put(McpSchema.METHOD_TOOLS_CALL, this::handleToolsCall);
		methodHandlers.put(McpSchema.METHOD_RESOURCES_LIST, this::handleResourcesList);
		methodHandlers.put(McpSchema.METHOD_RESOURCES_READ, this::handleResourcesRead);
		methodHandlers.put(McpSchema.METHOD_RESOURCES_TEMPLATES_LIST, this::handleResourcesTemplatesList);
		methodHandlers.put(McpSchema.METHOD_RESOURCES_SUBSCRIBE, this::handleResourcesSubscribe);
		methodHandlers.put(McpSchema.METHOD_RESOURCES_UNSUBSCRIBE, this::handleResourcesUnsubscribe);
		methodHandlers.put(McpSchema.METHOD_PROMPT_LIST, this::handlePromptList);
		methodHandlers.put(McpSchema.METHOD_PROMPT_GET, this::handlePromptGet);
		methodHandlers.put(McpSchema.METHOD_LOGGING_SET_LEVEL, this::handleLoggingSetLevel);
		methodHandlers.put(McpSchema.METHOD_COMPLETION_COMPLETE, this::handleCompletionComplete);
	}

	/**
	 * 注册自定义 method handler，覆盖或扩展默认 handler。
	 *
	 * @param method  JSON-RPC method 名（如 {@code "tools/list"}）
	 * @param handler 自定义 handler，不能为 null
	 * @return this
	 */
	public McpServer methodHandler(String method, McpRequestHandler handler) {
		if (StrUtil.isBlank(method)) {
			throw new IllegalArgumentException("Method must not be blank");
		}
		Objects.requireNonNull(handler, "Handler must not be null");
		methodHandlers.put(method, handler);
		return this;
	}

	// ============================================================
	//  内置 method handler
	// ============================================================

	private JsonRpcResponse handleInitialize(McpServerSession session, JsonRpcRequest request) {
		McpInitializeRequest params = JsonUtil.convertValue(request.getParams(), McpInitializeRequest.class);
		McpInitializeResult result = new McpInitializeResult();
		result.setProtocolVersion(params == null ? McpSchema.LATEST_PROTOCOL_VERSION : params.getProtocolVersion());
		result.setCapabilities(serverCapabilities);
		result.setServerInfo(serverInfo);
		return successResponse(request.getId(), result);
	}

	private JsonRpcResponse handlePing(McpServerSession session, JsonRpcRequest request) {
		return successResponse(request.getId(), Collections.emptyMap());
	}

	private JsonRpcResponse handleToolsList(McpServerSession session, JsonRpcRequest request) {
		McpListToolsResult result = new McpListToolsResult();
		List<McpTool> toolDefs = new ArrayList<>(tools.size());
		for (McpToolSpecification spec : tools.values()) {
			toolDefs.add(spec.getTool());
		}
		result.setTools(toolDefs);
		return successResponse(request.getId(), result);
	}

	private JsonRpcResponse handleToolsCall(McpServerSession session, JsonRpcRequest request) {
		McpCallToolRequest params = JsonUtil.convertValue(request.getParams(), McpCallToolRequest.class);
		if (params == null || StrUtil.isBlank(params.getName())) {
			throw new McpException(JsonRpcErrorCodes.INVALID_PARAMS, "Missing tool name");
		}
		String name = params.getName();
		McpToolSpecification spec = tools.get(name);
		if (spec == null) {
			throw new McpException(McpErrorCodes.TOOL_NOT_FOUND, "Tool not found: " + name);
		}
		Map<String, Object> arguments = getCallToolArguments(params.getArguments());
		McpCallToolResult result;
		try {
			if (spec.isStream()) {
				McpServerSession streamSession = session != null ? session : new McpServerSession("stateless", null);
				result = streamSession.callToolStream(spec, arguments,
					Boolean.TRUE.equals(spec.getTool().getReturnDirect()));
			} else {
				result = spec.getCall().apply(session, arguments);
			}
		} catch (McpException e) {
			throw e;
		} catch (Exception e) {
			log.error("Tool execution failed: {}", name, e);
			McpCallToolResult errorResult = new McpCallToolResult();
			errorResult.setContent(Collections.singletonList(new McpTextContent(e.getMessage())));
			errorResult.setError(Boolean.TRUE);
			return successResponse(request.getId(), errorResult);
		}
		if (result == null) {
			throw new McpException(JsonRpcErrorCodes.INTERNAL_ERROR, "Tool returned null: " + name);
		}
		return successResponse(request.getId(), result);
	}

	private JsonRpcResponse handleResourcesList(McpServerSession session, JsonRpcRequest request) {
		McpListResourcesResult result = new McpListResourcesResult();
		List<McpResource> list = new ArrayList<>(resources.size());
		for (McpResourceSpecification spec : resources.values()) {
			list.add(spec.getResource());
		}
		result.setResources(list);
		return successResponse(request.getId(), result);
	}

	private JsonRpcResponse handleResourcesRead(McpServerSession session, JsonRpcRequest request) {
		McpReadResourceRequest params = JsonUtil.convertValue(request.getParams(), McpReadResourceRequest.class);
		if (params == null || StrUtil.isBlank(params.getUri())) {
			throw new McpException(JsonRpcErrorCodes.INVALID_PARAMS, "Missing resource uri");
		}
		String uri = params.getUri();
		// 1) exact match
		McpResourceSpecification spec = findResourceByUri(uri);
		if (spec != null && spec.getReadHandler() != null) {
			McpReadResourceResult result = spec.getReadHandler().apply(session, params);
			return successResponse(request.getId(), result);
		}
		// 2) template match
		for (McpResourceTemplateSpecification template : resourceTemplates.values()) {
			String templateUri = template.getResource().getUriTemplate();
			if (StrUtil.isBlank(templateUri) || template.getReadHandler() == null) {
				continue;
			}
			try {
				if (new UriTemplate(templateUri).matchesTemplate(uri)) {
					McpReadResourceResult result = template.getReadHandler().apply(session, params);
					return successResponse(request.getId(), result);
				}
			} catch (Exception e) {
				log.warn("Invalid resource template uri: {}", templateUri, e);
			}
		}
		throw new McpException(McpErrorCodes.RESOURCE_NOT_FOUND, "Resource not found: " + uri);
	}

	private McpResourceSpecification findResourceByUri(String uri) {
		McpResourceSpecification spec = resources.get(uri);
		if (spec != null) {
			return spec;
		}
		for (McpResourceSpecification r : resources.values()) {
			if (uri.equals(r.getResource().getUri())) {
				return r;
			}
		}
		return null;
	}

	private JsonRpcResponse handleResourcesTemplatesList(McpServerSession session, JsonRpcRequest request) {
		McpListResourceTemplatesResult result = new McpListResourceTemplatesResult();
		List<McpResourceTemplate> list = new ArrayList<>(resourceTemplates.size());
		for (McpResourceTemplateSpecification spec : resourceTemplates.values()) {
			list.add(spec.getResource());
		}
		result.setResourceTemplates(list);
		return successResponse(request.getId(), result);
	}

	private JsonRpcResponse handleResourcesSubscribe(McpServerSession session, JsonRpcRequest request) {
		McpSubscribeRequest params = JsonUtil.convertValue(request.getParams(), McpSubscribeRequest.class);
		if (params == null || StrUtil.isBlank(params.getUri())) {
			throw new McpException(JsonRpcErrorCodes.INVALID_PARAMS, "Missing resource uri");
		}
		// 订阅状态由 transport 层负责追踪（MCP 标准要求发送 notifications/resources/updated）
		log.debug("Resource subscribe: sessionId={}, uri={}", session.getSessionId(), params.getUri());
		return successResponse(request.getId(), Collections.emptyMap());
	}

	private JsonRpcResponse handleResourcesUnsubscribe(McpServerSession session, JsonRpcRequest request) {
		McpUnsubscribeRequest params = JsonUtil.convertValue(request.getParams(), McpUnsubscribeRequest.class);
		if (params == null || StrUtil.isBlank(params.getUri())) {
			throw new McpException(JsonRpcErrorCodes.INVALID_PARAMS, "Missing resource uri");
		}
		log.debug("Resource unsubscribe: sessionId={}, uri={}", session.getSessionId(), params.getUri());
		return successResponse(request.getId(), Collections.emptyMap());
	}

	private JsonRpcResponse handlePromptList(McpServerSession session, JsonRpcRequest request) {
		McpListPromptsResult result = new McpListPromptsResult();
		List<McpPrompt> list = new ArrayList<>(prompts.size());
		for (McpPromptSpecification spec : prompts.values()) {
			list.add(spec.getPrompt());
		}
		result.setPrompts(list);
		return successResponse(request.getId(), result);
	}

	private JsonRpcResponse handlePromptGet(McpServerSession session, JsonRpcRequest request) {
		McpGetPromptRequest params = JsonUtil.convertValue(request.getParams(), McpGetPromptRequest.class);
		if (params == null || StrUtil.isBlank(params.getName())) {
			throw new McpException(JsonRpcErrorCodes.INVALID_PARAMS, "Missing prompt name");
		}
		McpPromptSpecification spec = prompts.get(params.getName());
		if (spec == null || spec.getPromptHandler() == null) {
			throw new McpException(McpErrorCodes.PROMPT_NOT_FOUND, "Prompt not found: " + params.getName());
		}
		McpGetPromptResult result = spec.getPromptHandler().apply(session, params);
		return successResponse(request.getId(), result);
	}

	private JsonRpcResponse handleLoggingSetLevel(McpServerSession session, JsonRpcRequest request) {
		McpSetLevelRequest params = JsonUtil.convertValue(request.getParams(), McpSetLevelRequest.class);
		if (params == null || params.getLevel() == null) {
			throw new McpException(JsonRpcErrorCodes.INVALID_PARAMS, "Missing logging level");
		}
		sessionLogLevels.put(session.getSessionId(), params.getLevel());
		return successResponse(request.getId(), Collections.emptyMap());
	}

	private JsonRpcResponse handleCompletionComplete(McpServerSession session, JsonRpcRequest request) {
		McpCompleteRequest params = JsonUtil.convertValue(request.getParams(), McpCompleteRequest.class);
		if (params == null || params.getRef() == null) {
			throw new McpException(JsonRpcErrorCodes.INVALID_PARAMS, "Missing complete reference");
		}
		McpCompleteResult result = new McpCompleteResult();
		result.setValues(Collections.emptyList());
		result.setTotal(0);
		result.setHasMore(false);
		return successResponse(request.getId(), result);
	}

	// ============================================================
	//  日志通知辅助（供 transport 调用，向 client 推送 notifications/message）
	// ============================================================

	/**
	 * 获取某个 session 设置的日志级别。
	 *
	 * @param sessionId sessionId
	 * @return McpLoggingLevel，可能为 null
	 */
	public McpLoggingLevel getSessionLogLevel(String sessionId) {
		return sessionLogLevels.get(sessionId);
	}

	/**
	 * 清除某个 session 的日志级别。
	 *
	 * @param sessionId sessionId
	 */
	public void clearSessionLogLevel(String sessionId) {
		if (sessionId != null) {
			sessionLogLevels.remove(sessionId);
		}
	}

	// ============================================================
	//  Session 注册表（供 transport 注册/注销，供业务侧推送通知）
	// ============================================================

	/**
	 * 注册一个 session 到中央注册表，由 transport 创建 session 时调用。
	 *
	 * @param session session
	 */
	public void registerSession(McpServerSession session) {
		if (session != null) {
			sessionRegistry.put(session.getSessionId(), session);
		}
	}

	/**
	 * 从中央注册表注销一个 session。
	 *
	 * @param sessionId sessionId
	 */
	public void unregisterSession(String sessionId) {
		if (sessionId == null) {
			return;
		}
		sessionRegistry.remove(sessionId);
		sessionLogLevels.remove(sessionId);
	}

	/**
	 * 根据 sessionId 查找 session。
	 *
	 * @param sessionId sessionId
	 * @return McpServerSession or null
	 */
	public McpServerSession getSession(String sessionId) {
		if (sessionId == null) {
			return null;
		}
		McpServerSession s = sessionRegistry.get(sessionId);
		return s != null && s.hasStream() ? s : null;
	}

	// ============================================================
	//  Server → Client 通知辅助
	// ============================================================

	/**
	 * 发送 {@code notifications/message} 日志通知给指定 session。
	 * 会自动按 session 级日志级别过滤。
	 *
	 * @param sessionId sessionId
	 * @param level     log level
	 * @param logger    logger name
	 * @param data      log data
	 */
	public void sendLoggingMessage(String sessionId, McpLoggingLevel level, String logger, Object data) {
		McpServerSession session = getSession(sessionId);
		if (session == null) {
			return;
		}
		McpLoggingLevel sessionLevel = sessionLogLevels.get(sessionId);
		if (sessionLevel != null && level.ordinal() < sessionLevel.ordinal()) {
			return;
		}
		Map<String, Object> params = new HashMap<>(4);
		params.put("level", level.name().toLowerCase());
		if (logger != null) {
			params.put("logger", logger);
		}
		params.put("data", data);
		session.sendNotification("notifications/message", params);
	}

	/**
	 * 发送 {@code notifications/progress} 进度通知。
	 *
	 * @param sessionId     sessionId
	 * @param progressToken 进度 token，与初始请求时使用的 token 对应
	 * @param progress      当前进度（0~1 或自定义单位）
	 * @param total         总进度，可为 null
	 * @param message       描述，可为 null
	 */
	public void sendProgress(String sessionId, String progressToken, double progress, Double total, String message) {
		McpServerSession session = getSession(sessionId);
		if (session == null) {
			return;
		}
		Map<String, Object> params = new HashMap<>(4);
		params.put("progressToken", progressToken);
		params.put("progress", progress);
		if (total != null) {
			params.put("total", total);
		}
		if (message != null) {
			params.put("message", message);
		}
		session.sendNotification("notifications/progress", params);
	}

	/**
	 * 发送 {@code notifications/resources/updated} 通知，告知 client 资源内容发生变化。
	 *
	 * @param sessionId sessionId
	 * @param uri       资源 URI
	 */
	public void sendResourcesUpdated(String sessionId, String uri) {
		McpServerSession session = getSession(sessionId);
		if (session == null) {
			return;
		}
		Map<String, Object> params = new HashMap<>(1);
		params.put("uri", uri);
		session.sendNotification("notifications/resources/updated", params);
	}

	/**
	 * 发送 {@code notifications/roots/list_changed} 通知。
	 *
	 * @param sessionId sessionId
	 */
	public void sendRootsListChanged(String sessionId) {
		McpServerSession session = getSession(sessionId);
		if (session == null) {
			return;
		}
		session.sendNotification("notifications/roots/list_changed", Collections.emptyMap());
	}

	/**
	 * 广播 {@code notifications/tools/list_changed} 给所有 session。
	 */
	public void broadcastToolsListChanged() {
		Map<String, Object> params = Collections.emptyMap();
		for (McpServerSession session : sessionRegistry.values()) {
			session.sendNotification("notifications/tools/list_changed", params);
		}
	}

	/**
	 * 广播 {@code notifications/prompts/list_changed} 给所有 session。
	 */
	public void broadcastPromptsListChanged() {
		Map<String, Object> params = Collections.emptyMap();
		for (McpServerSession session : sessionRegistry.values()) {
			session.sendNotification("notifications/prompts/list_changed", params);
		}
	}

	/**
	 * 广播 {@code notifications/resources/list_changed} 给所有 session。
	 */
	public void broadcastResourcesListChanged() {
		Map<String, Object> params = Collections.emptyMap();
		for (McpServerSession session : sessionRegistry.values()) {
			session.sendNotification("notifications/resources/list_changed", params);
		}
	}

	/**
	 * 发送 {@code notifications/cancelled} 取消通知（server → client）。
	 *
	 * @param sessionId sessionId
	 * @param requestId 要取消的请求 id
	 * @param reason    原因
	 */
	public void sendCancelled(String sessionId, Object requestId, String reason) {
		McpServerSession session = getSession(sessionId);
		if (session == null) {
			return;
		}
		Map<String, Object> params = new HashMap<>(2);
		params.put("requestId", requestId);
		if (reason != null) {
			params.put("reason", reason);
		}
		session.sendNotification("notifications/cancelled", params);
	}

	// ============================================================
	//  请求分发入口
	// ============================================================

	/**
	 * 解码 JSON-RPC 消息。
	 *
	 * @param requestBody requestBody
	 * @return JsonRpcMessage
	 */
	public static JsonRpcMessage deserializeJsonRpcMessage(byte[] requestBody) {
		Map<String, Object> map = JsonUtil.readValue(requestBody, Map.class);
		String jsonText = new String(requestBody);
		log.debug("Received JSON message: {}", jsonText);
		if (map.containsKey("method") && map.containsKey("id")) {
			return JsonUtil.convertValue(map, JsonRpcRequest.class);
		} else if (map.containsKey("method")) {
			return JsonUtil.convertValue(map, JsonRpcNotification.class);
		} else if (map.containsKey("result") || map.containsKey("error")) {
			return JsonUtil.convertValue(map, JsonRpcResponse.class);
		}
		throw new IllegalArgumentException("Cannot deserialize JsonRpcMessage: " + jsonText);
	}

	/**
	 * 解析 call tool 参数。
	 *
	 * @param arguments arguments
	 * @return 参数 map
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> getCallToolArguments(Object arguments) {
		if (arguments == null) {
			return null;
		}
		if (arguments instanceof Map) {
			return (Map<String, Object>) arguments;
		}
		throw new McpException(JsonRpcErrorCodes.INVALID_PARAMS,
			"Tool arguments must be a JSON object, got: " + arguments.getClass().getSimpleName());
	}

	private static JsonRpcResponse successResponse(Object id, Object result) {
		JsonRpcResponse response = new JsonRpcResponse();
		response.setJsonrpc(McpSchema.JSONRPC_VERSION);
		response.setId(id);
		response.setResult(result);
		return response;
	}

	private static JsonRpcResponse errorResponse(Object id, int code, String message) {
		return errorResponse(id, code, message, null);
	}

	private static JsonRpcResponse errorResponse(Object id, int code, String message, Object data) {
		JsonRpcError error = new JsonRpcError();
		error.setCode(code);
		error.setMessage(message);
		if (data != null) {
			error.setData(data);
		}
		JsonRpcResponse response = new JsonRpcResponse();
		response.setJsonrpc(McpSchema.JSONRPC_VERSION);
		response.setId(id);
		response.setError(error);
		return response;
	}

	/**
	 * 处理 incoming JSON-RPC 请求，统一捕获所有异常并转换为 JSON-RPC error 响应。
	 *
	 * <p>此方法保证不会抛出异常，永远返回 {@link JsonRpcResponse}。</p>
	 *
	 * @param session McpServerSession
	 * @param request incoming JSON-RPC request
	 * @return JsonRpcResponse
	 */
	public JsonRpcResponse handleIncomingRequest(McpServerSession session, JsonRpcRequest request) {
		if (request == null) {
			return errorResponse(null, JsonRpcErrorCodes.INVALID_REQUEST, "Request must not be null");
		}
		String method = request.getMethod();
		if (StrUtil.isBlank(method)) {
			return errorResponse(request.getId(), JsonRpcErrorCodes.INVALID_REQUEST, "Missing method");
		}
		if (session != null) {
			session.touch();
		}
		McpRequestHandler handler = methodHandlers.get(method);
		if (handler == null) {
			return errorResponse(request.getId(), JsonRpcErrorCodes.METHOD_NOT_FOUND, "Method not found: " + method);
		}
		try {
			return handler.handle(session, request);
		} catch (McpException e) {
			log.warn("MCP handler error: method={}, code={}, message={}", method, e.getCode(), e.getMessage());
			return errorResponse(request.getId(), e.getCode(), e.getMessage(), e.getData());
		} catch (Exception e) {
			log.error("Unhandled MCP error for method: " + method, e);
			return errorResponse(request.getId(), JsonRpcErrorCodes.INTERNAL_ERROR,
				"Internal error: " + e.getMessage());
		}
	}

	// ============================================================
	//  Builder API
	// ============================================================

	/**
	 * Sets the server implementation information that will be shared with clients
	 * during connection initialization. This helps with version compatibility,
	 * debugging, and server identification.
	 *
	 * @param serverInfo The server implementation details including name and version.
	 *                   Must not be null.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if serverInfo is null
	 */
	public McpServer serverInfo(McpImplementation serverInfo) {
		Objects.requireNonNull(serverInfo, "Server info must not be null");
		this.serverInfo = serverInfo;
		return this;
	}

	/**
	 * Sets the server implementation information using name and version strings. This
	 * is a convenience method alternative to
	 * {@link #serverInfo(McpImplementation)}.
	 *
	 * @param name    The server name. Must not be null or empty.
	 * @param version The server version. Must not be null or empty.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if name or version is null or empty
	 * @see #serverInfo(McpImplementation)
	 */
	public McpServer serverInfo(String name, String version) {
		if (StrUtil.isBlank(name)) {
			throw new IllegalArgumentException("Server info name must not be blank");
		}
		if (StrUtil.isBlank(version)) {
			throw new IllegalArgumentException("Server info version must not be blank");
		}
		this.serverInfo = new McpImplementation(name, version);
		return this;
	}

	/**
	 * Sets the server capabilities that will be advertised to clients during
	 * connection initialization. Capabilities define what features the server
	 * supports, such as:
	 * <ul>
	 * <li>Tool execution
	 * <li>Resource access
	 * <li>Prompt handling
	 * </ul>
	 *
	 * @param serverCapabilities The server capabilities configuration. Must not be
	 *                           null.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if serverCapabilities is null
	 */
	public McpServer capabilities(McpServerCapabilities serverCapabilities) {
		Objects.requireNonNull(serverCapabilities, "Server capabilities must not be null");
		this.serverCapabilities = serverCapabilities;
		return this;
	}

	/**
	 * Adds a single tool with its implementation handler to the server.
	 *
	 * @param tool    The tool definition including name, description, and schema. Must
	 *                not be null.
	 * @param handler The function that implements the tool's logic. Must not be null.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if tool or handler is null
	 */
	public McpServer tool(McpTool tool,
	                      BiFunction<McpServerSession, Map<String, Object>, McpCallToolResult> handler) {
		Objects.requireNonNull(tool, "Tool must not be null");
		Objects.requireNonNull(handler, "Handler must not be null");
		String name = tool.getName();
		if (StrUtil.isBlank(name)) {
			throw new IllegalArgumentException("Tool name must not be blank");
		}
		this.tools.put(name, McpToolSpecification.of(tool, handler));
		return this;
	}

	/**
	 * Adds a single tool with its streaming implementation handler to the server.
	 *
	 * @param tool    The tool definition including name, description, and schema. Must
	 *                not be null.
	 * @param handler The function that implements the tool's streaming logic. Must not
	 *                be null.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if tool or handler is null
	 */
	public McpServer toolStream(McpTool tool,
	                            BiFunction<McpServerSession, Map<String, Object>, Iterator<McpContent>> handler) {
		Objects.requireNonNull(tool, "Tool must not be null");
		Objects.requireNonNull(handler, "Handler must not be null");
		String name = tool.getName();
		if (StrUtil.isBlank(name)) {
			throw new IllegalArgumentException("Tool name must not be blank");
		}
		this.tools.put(name, McpToolSpecification.ofStream(tool, handler));
		return this;
	}

	/**
	 * Adds multiple tools with their handlers to the server using a List.
	 *
	 * @param toolSpecifications The list of tool specifications to add. Must not be
	 *                           null.
	 * @return This builder instance for method chaining
	 */
	public McpServer tools(List<McpToolSpecification> toolSpecifications) {
		Objects.requireNonNull(toolSpecifications, "Tool handlers list must not be null");
		for (McpToolSpecification spec : toolSpecifications) {
			if (spec == null || spec.getTool() == null) {
				continue;
			}
			this.tools.put(spec.getTool().getName(), spec);
		}
		return this;
	}

	/**
	 * Adds multiple tools with their handlers to the server using varargs.
	 *
	 * @param toolSpecifications The tool specifications to add. Must not be null.
	 * @return This builder instance for method chaining
	 */
	public McpServer tools(McpToolSpecification... toolSpecifications) {
		Objects.requireNonNull(toolSpecifications, "Tool handlers list must not be null");
		return tools(Arrays.asList(toolSpecifications));
	}

	/**
	 * Registers multiple resources with their handlers using a Map.
	 *
	 * @param resourceSpecifications Map of resource name to specification. Must not
	 *                               be null.
	 * @return This builder instance for method chaining
	 */
	public McpServer resources(
		Map<String, McpResourceSpecification> resourceSpecifications) {
		Objects.requireNonNull(resourceSpecifications, "Resource handlers map must not be null");
		for (Map.Entry<String, McpResourceSpecification> e : resourceSpecifications.entrySet()) {
			McpResourceSpecification spec = e.getValue();
			if (spec == null || spec.getResource() == null) {
				continue;
			}
			this.resources.put(spec.getResource().getUri(), spec);
		}
		return this;
	}

	/**
	 * Registers multiple resources with their handlers using a List.
	 *
	 * @param resourceSpecifications List of resource specifications. Must not be
	 *                               null.
	 * @return This builder instance for method chaining
	 */
	public McpServer resources(List<McpResourceSpecification> resourceSpecifications) {
		Objects.requireNonNull(resourceSpecifications, "Resource handlers list must not be null");
		for (McpResourceSpecification spec : resourceSpecifications) {
			if (spec == null || spec.getResource() == null) {
				continue;
			}
			this.resources.put(spec.getResource().getUri(), spec);
		}
		return this;
	}

	/**
	 * Registers multiple resources with their handlers using varargs.
	 *
	 * @param resourceSpecifications The resource specifications to add. Must not be
	 *                               null.
	 * @return This builder instance for method chaining
	 */
	public McpServer resources(McpResourceSpecification... resourceSpecifications) {
		Objects.requireNonNull(resourceSpecifications, "Resource handlers list must not be null");
		return resources(Arrays.asList(resourceSpecifications));
	}

	/**
	 * Sets the resource templates that define patterns for dynamic resource access.
	 *
	 * @param resourceTemplates List of resource templates. If null, clears existing
	 *                          templates.
	 * @return This builder instance for method chaining
	 */
	public McpServer resourceTemplates(List<McpResourceTemplateSpecification> resourceTemplates) {
		Objects.requireNonNull(resourceTemplates, "Resource templates must not be null");
		for (McpResourceTemplateSpecification spec : resourceTemplates) {
			if (spec == null || spec.getResource() == null) {
				continue;
			}
			this.resourceTemplates.put(spec.getResource().getUriTemplate(), spec);
		}
		return this;
	}

	/**
	 * Sets the resource templates using varargs for convenience.
	 *
	 * @param resourceTemplates The resource templates to set.
	 * @return This builder instance for method chaining
	 */
	public McpServer resourceTemplates(McpResourceTemplateSpecification... resourceTemplates) {
		Objects.requireNonNull(resourceTemplates, "Resource templates must not be null");
		return resourceTemplates(Arrays.asList(resourceTemplates));
	}

	/**
	 * Registers multiple prompts with their handlers using a Map.
	 *
	 * @param prompts Map of prompt name to specification. Must not be null.
	 * @return This builder instance for method chaining
	 */
	public McpServer prompts(Map<String, McpPromptSpecification> prompts) {
		Objects.requireNonNull(prompts, "Prompts map must not be null");
		for (Map.Entry<String, McpPromptSpecification> e : prompts.entrySet()) {
			McpPromptSpecification spec = e.getValue();
			if (spec == null || spec.getPrompt() == null) {
				continue;
			}
			this.prompts.put(spec.getPrompt().getName(), spec);
		}
		return this;
	}

	/**
	 * Registers multiple prompts with their handlers using a List.
	 *
	 * @param prompts List of prompt specifications. Must not be null.
	 * @return This builder instance for method chaining
	 */
	public McpServer prompts(List<McpPromptSpecification> prompts) {
		Objects.requireNonNull(prompts, "Prompts list must not be null");
		for (McpPromptSpecification spec : prompts) {
			if (spec == null || spec.getPrompt() == null) {
				continue;
			}
			this.prompts.put(spec.getPrompt().getName(), spec);
		}
		return this;
	}

	/**
	 * Registers multiple prompts with their handlers using varargs.
	 *
	 * @param prompts The prompt specifications to add. Must not be null.
	 * @return This builder instance for method chaining
	 */
	public McpServer prompts(McpPromptSpecification... prompts) {
		Objects.requireNonNull(prompts, "Prompts list must not be null");
		return prompts(Arrays.asList(prompts));
	}

	/**
	 * Registers a consumer that will be notified when the list of roots changes.
	 *
	 * @param handler The handler to register. Must not be null.
	 * @return This builder instance for method chaining
	 */
	public McpServer rootsChangeHandler(BiConsumer<McpServerSession, List<McpRoot>> handler) {
		Objects.requireNonNull(handler, "Consumer must not be null");
		this.rootsChangeHandlers.add(handler);
		return this;
	}

	/**
	 * Registers multiple consumers that will be notified when the list of roots
	 * changes.
	 *
	 * @param handlers The list of handlers to register. Must not be null.
	 * @return This builder instance for method chaining
	 */
	public McpServer rootsChangeHandlers(List<BiConsumer<McpServerSession, List<McpRoot>>> handlers) {
		Objects.requireNonNull(handlers, "Handlers list must not be null");
		this.rootsChangeHandlers.addAll(handlers);
		return this;
	}

	/**
	 * Registers multiple consumers using varargs.
	 *
	 * @param handlers The handlers to register. Must not be null.
	 * @return This builder instance for method chaining
	 */
	@SafeVarargs
	public final McpServer rootsChangeHandlers(BiConsumer<McpServerSession, List<McpRoot>>... handlers) {
		Objects.requireNonNull(handlers, "Handlers list must not be null");
		return this.rootsChangeHandlers(Arrays.asList(handlers));
	}

	/**
	 * 注册传输层。
	 *
	 * @param transport 传输层实现
	 * @return this
	 */
	public McpServer useTransport(McpTransport transport) {
		Objects.requireNonNull(transport, "Transport must not be null");
		this.transports.add(transport);
		return this;
	}

	/**
	 * 使用 SSE 传输层（便捷方法）。
	 *
	 * @return this
	 */
	public McpServer useSseTransport() {
		return useTransport(new SseTransport(this));
	}

	/**
	 * 使用 SSE 传输层（便捷方法）。
	 *
	 * @param sseEndpoint     sseEndpoint
	 * @param messageEndpoint messageEndpoint
	 * @return this
	 */
	public McpServer useSseTransport(String sseEndpoint, String messageEndpoint) {
		return useTransport(new SseTransport(this, sseEndpoint, messageEndpoint));
	}

	/**
	 * 使用 Streamable HTTP 传输层（便捷方法）。
	 *
	 * @return this
	 */
	public McpServer useStreamableTransport() {
		return useTransport(new StreamableHttpTransport(this));
	}

	/**
	 * 使用 Streamable HTTP 传输层（便捷方法）。
	 *
	 * @param endpoint endpoint
	 * @return this
	 */
	public McpServer useStreamableTransport(String endpoint) {
		return useTransport(new StreamableHttpTransport(this, endpoint));
	}

	/**
	 * 根据请求路径分发到对应的传输层处理。
	 *
	 * @param request HttpRequest
	 * @return HttpResponse
	 */
	public HttpResponse handleRequest(HttpRequest request) {
		for (McpTransport transport : transports) {
			HttpResponse response = transport.handle(request);
			// 如果不是 404，说明找到了对应的 transport
			if (response.getStatus() != HttpResponseStatus.C404) {
				return response;
			}
		}
		// 没有找到对应的 transport
		HttpResponse resp = new HttpResponse(request);
		resp.setStatus(HttpResponseStatus.C404);
		return resp;
	}

	/**
	 * 获取已注册的传输层列表。
	 *
	 * @return transports
	 */
	public List<McpTransport> getTransports() {
		return Collections.unmodifiableList(transports);
	}

	/**
	 * 向所有 transport 的所有 session 发送心跳。
	 */
	public void sendHeartbeat() {
		for (McpTransport transport : transports) {
			transport.sendHeartbeat();
		}
	}

	/**
	 * 注册路由
	 * @param router HttpRouter
	 */
	public void registerRoute(HttpRouter router) {
		for (McpTransport transport : getTransports()) {
			if (transport instanceof SseTransport) {
				SseTransport sseTransport = (SseTransport) transport;
				router.get(sseTransport.getSseEndpoint(), sseTransport::handleSseConnection);
				router.post(sseTransport.getMessageEndpoint(), sseTransport::handleMessage);
			} else if (transport instanceof StreamableHttpTransport) {
				StreamableHttpTransport streamTransport = (StreamableHttpTransport) transport;
				router.route(streamTransport.getEndpoint(), streamTransport::handle);
			}
		}
	}
}
