package org.tio.http.mcp.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.http.common.HttpResponseStatus;
import org.tio.http.jsonrpc.JsonRpcMessage;
import org.tio.http.jsonrpc.JsonRpcNotification;
import org.tio.http.jsonrpc.JsonRpcRequest;
import org.tio.http.jsonrpc.JsonRpcResponse;
import org.tio.http.mcp.schema.*;
import org.tio.http.common.stream.HttpStream;
import org.tio.utils.hutool.StrUtil;
import org.tio.utils.json.JsonUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * mcp 服务
 *
 * @author L.cm
 */
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
	 * 默认的服务信息
	 */
	private static final McpImplementation DEFAULT_SERVER_INFO = new McpImplementation("mcp-server", "1.0.0");

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
		this.sseEndpoint = StrUtil.isBlank(sseEndpoint) ? DEFAULT_SSE_ENDPOINT : sseEndpoint;
		this.messageEndpoint = StrUtil.isBlank(messageEndpoint) ? DEFAULT_MESSAGE_ENDPOINT : messageEndpoint;
	}

	private McpImplementation serverInfo = DEFAULT_SERVER_INFO;
	private McpServerCapabilities serverCapabilities;

	/**
	 * The Model Context Protocol (MCP) allows servers to expose tools that can be
	 * invoked by language models. Tools enable models to interact with external
	 * systems, such as querying databases, calling APIs, or performing computations.
	 * Each tool is uniquely identified by a name and includes metadata describing its
	 * schema.
	 */
	private final List<McpToolSpecification> tools = new ArrayList<>();

	/**
	 * The Model Context Protocol (MCP) provides a standardized way for servers to
	 * expose resources to clients. Resources allow servers to share data that
	 * provides context to language models, such as files, database schemas, or
	 * application-specific information. Each resource is uniquely identified by a
	 * URI.
	 */
	private final Map<String, McpResourceSpecification> resources = new HashMap<>();

	private final Map<String, McpResourceTemplateSpecification> resourceTemplates = new HashMap<>();

	/**
	 * The Model Context Protocol (MCP) provides a standardized way for servers to
	 * expose prompt templates to clients. Prompts allow servers to provide structured
	 * messages and instructions for interacting with language models. Clients can
	 * discover available prompts, retrieve their contents, and provide arguments to
	 * customize them.
	 */
	private final Map<String, McpPromptSpecification> prompts = new HashMap<>();

	private final List<BiConsumer<McpServerSession, List<McpRoot>>> rootsChangeHandlers = new ArrayList<>();

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
	 * Adds a single tool with its implementation handler to the server. This is a
	 * convenience method for registering individual tools without creating a
	 * {@link McpToolSpecification} explicitly.
	 *
	 * <p>
	 * Example usage: <pre>{@code
	 * .tool(
	 *     new Tool("calculator", "Performs calculations", schema),
	 *     (exchange, args) -> new CallToolResult("Result: " + calculate(args))
	 * )
	 * }</pre>
	 *
	 * @param tool    The tool definition including name, description, and schema. Must
	 *                not be null.
	 * @param handler The function that implements the tool's logic. Must not be null.
	 *                The function's first argument is an {@link McpServer} upon which
	 *                the server can interact with the connected client. The second argument is the
	 *                list of arguments passed to the tool.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if tool or handler is null
	 */
	public McpServer tool(McpTool tool,
						  BiFunction<McpServerSession, Map<String, Object>, McpCallToolResult> handler) {
		Objects.requireNonNull(tool, "Tool must not be null");
		Objects.requireNonNull(handler, "Handler must not be null");
		this.tools.add(new McpToolSpecification(tool, handler));
		return this;
	}

	/**
	 * Adds multiple tools with their handlers to the server using a List. This method
	 * is useful when tools are dynamically generated or loaded from a configuration
	 * source.
	 *
	 * @param toolSpecifications The list of tool specifications to add. Must not be
	 *                           null.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if toolSpecifications is null
	 * @see #tools(McpToolSpecification...)
	 */
	public McpServer tools(List<McpToolSpecification> toolSpecifications) {
		Objects.requireNonNull(toolSpecifications, "Tool handlers list must not be null");
		this.tools.addAll(toolSpecifications);
		return this;
	}

	/**
	 * Adds multiple tools with their handlers to the server using varargs. This
	 * method provides a convenient way to register multiple tools inline.
	 *
	 * <p>
	 * Example usage: <pre>{@code
	 * .tools(
	 *     new ToolSpecification(calculatorTool, calculatorHandler),
	 *     new ToolSpecification(weatherTool, weatherHandler),
	 *     new ToolSpecification(fileManagerTool, fileManagerHandler)
	 * )
	 * }</pre>
	 *
	 * @param toolSpecifications The tool specifications to add. Must not be null.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if toolSpecifications is null
	 * @see #tools(List)
	 */
	public McpServer tools(McpToolSpecification... toolSpecifications) {
		Objects.requireNonNull(toolSpecifications, "Tool handlers list must not be null");
		this.tools.addAll(Arrays.asList(toolSpecifications));
		return this;
	}

	/**
	 * Registers multiple resources with their handlers using a Map. This method is
	 * useful when resources are dynamically generated or loaded from a configuration
	 * source.
	 *
	 * @param resourceSpecifications Map of resource name to specification. Must not
	 *                               be null.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if resourceSpecifications is null
	 * @see #resources(McpResourceSpecification...)
	 */
	public McpServer resources(
		Map<String, McpResourceSpecification> resourceSpecifications) {
		Objects.requireNonNull(resourceSpecifications, "Resource handlers map must not be null");
		this.resources.putAll(resourceSpecifications);
		return this;
	}

	/**
	 * Registers multiple resources with their handlers using a List. This method is
	 * useful when resources need to be added in bulk from a collection.
	 *
	 * @param resourceSpecifications List of resource specifications. Must not be
	 *                               null.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if resourceSpecifications is null
	 * @see #resources(McpResourceSpecification...)
	 */
	public McpServer resources(List<McpResourceSpecification> resourceSpecifications) {
		Objects.requireNonNull(resourceSpecifications, "Resource handlers list must not be null");
		for (McpResourceSpecification resource : resourceSpecifications) {
			this.resources.put(resource.getResource().getUri(), resource);
		}
		return this;
	}

	/**
	 * Registers multiple resources with their handlers using varargs. This method
	 * provides a convenient way to register multiple resources inline.
	 *
	 * <p>
	 * Example usage: <pre>{@code
	 * .resources(
	 *     new ResourceSpecification(fileResource, fileHandler),
	 *     new ResourceSpecification(dbResource, dbHandler),
	 *     new ResourceSpecification(apiResource, apiHandler)
	 * )
	 * }</pre>
	 *
	 * @param resourceSpecifications The resource specifications to add. Must not be
	 *                               null.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if resourceSpecifications is null
	 */
	public McpServer resources(McpResourceSpecification... resourceSpecifications) {
		Objects.requireNonNull(resourceSpecifications, "Resource handlers list must not be null");
		for (McpResourceSpecification resource : resourceSpecifications) {
			this.resources.put(resource.getResource().getUri(), resource);
		}
		return this;
	}

	/**
	 * Sets the resource templates that define patterns for dynamic resource access.
	 * Templates use URI patterns with placeholders that can be filled at runtime.
	 *
	 * <p>
	 * Example usage: <pre>{@code
	 * .resourceTemplates(
	 *     new ResourceTemplate("file://{path}", "Access files by path"),
	 *     new ResourceTemplate("db://{table}/{id}", "Access database records")
	 * )
	 * }</pre>
	 *
	 * @param resourceTemplates List of resource templates. If null, clears existing
	 *                          templates.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if resourceTemplates is null.
	 * @see #resourceTemplates(List<McpResourceTemplateSpecification>...)
	 */
	public McpServer resourceTemplates(List<McpResourceTemplateSpecification> resourceTemplates) {
		Objects.requireNonNull(resourceTemplates, "Resource templates must not be null");
		for (McpResourceTemplateSpecification resource : resourceTemplates) {
			this.resourceTemplates.put(resource.getResource().getUriTemplate(), resource);
		}
		return this;
	}

	/**
	 * Sets the resource templates using varargs for convenience. This is an
	 * alternative to {@link #resourceTemplates(List)}.
	 *
	 * @param resourceTemplates The resource templates to set.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if resourceTemplates is null
	 * @see #resourceTemplates(List)
	 */
	public McpServer resourceTemplates(McpResourceTemplateSpecification... resourceTemplates) {
		Objects.requireNonNull(resourceTemplates, "Resource templates must not be null");
		for (McpResourceTemplateSpecification resource : resourceTemplates) {
			this.resourceTemplates.put(resource.getResource().getUriTemplate(), resource);
		}
		return this;
	}

	/**
	 * Registers multiple prompts with their handlers using a Map. This method is
	 * useful when prompts are dynamically generated or loaded from a configuration
	 * source.
	 *
	 * <p>
	 * Example usage: <pre>{@code
	 * Map<String, PromptSpecification> prompts = new HashMap<>();
	 * prompts.put("analysis", new PromptSpecification(
	 *     new Prompt("analysis", "Code analysis template"),
	 *     (exchange, request) -> new GetPromptResult(generateAnalysisPrompt(request))
	 * ));
	 * .prompts(prompts)
	 * }</pre>
	 *
	 * @param prompts Map of prompt name to specification. Must not be null.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if prompts is null
	 */
	public McpServer prompts(Map<String, McpPromptSpecification> prompts) {
		Objects.requireNonNull(prompts, "Prompts map must not be null");
		this.prompts.putAll(prompts);
		return this;
	}

	/**
	 * Registers multiple prompts with their handlers using a List. This method is
	 * useful when prompts need to be added in bulk from a collection.
	 *
	 * @param prompts List of prompt specifications. Must not be null.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if prompts is null
	 * @see #prompts(McpPromptSpecification...)
	 */
	public McpServer prompts(List<McpPromptSpecification> prompts) {
		Objects.requireNonNull(prompts, "Prompts list must not be null");
		for (McpPromptSpecification prompt : prompts) {
			this.prompts.put(prompt.getPrompt().getName(), prompt);
		}
		return this;
	}

	/**
	 * Registers multiple prompts with their handlers using varargs. This method
	 * provides a convenient way to register multiple prompts inline.
	 *
	 * <p>
	 * Example usage: <pre>{@code
	 * .prompts(
	 *     new PromptSpecification(analysisPrompt, analysisHandler),
	 *     new PromptSpecification(summaryPrompt, summaryHandler),
	 *     new PromptSpecification(reviewPrompt, reviewHandler)
	 * )
	 * }</pre>
	 *
	 * @param prompts The prompt specifications to add. Must not be null.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if prompts is null
	 */
	public McpServer prompts(McpPromptSpecification... prompts) {
		Objects.requireNonNull(prompts, "Prompts list must not be null");
		for (McpPromptSpecification prompt : prompts) {
			this.prompts.put(prompt.getPrompt().getName(), prompt);
		}
		return this;
	}

	/**
	 * Registers a consumer that will be notified when the list of roots changes. This
	 * is useful for updating resource availability dynamically, such as when new
	 * files are added or removed.
	 *
	 * @param handler The handler to register. Must not be null. The function's first
	 *                argument is an {@link McpServerSession} upon which the server can interact
	 *                with the connected client. The second argument is the list of roots.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if consumer is null
	 */
	public McpServer rootsChangeHandler(BiConsumer<McpServerSession, List<McpRoot>> handler) {
		Objects.requireNonNull(handler, "Consumer must not be null");
		this.rootsChangeHandlers.add(handler);
		return this;
	}

	/**
	 * Registers multiple consumers that will be notified when the list of roots
	 * changes. This method is useful when multiple consumers need to be registered at
	 * once.
	 *
	 * @param handlers The list of handlers to register. Must not be null.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if consumers is null
	 * @see #rootsChangeHandler(BiConsumer)
	 */
	public McpServer rootsChangeHandlers(List<BiConsumer<McpServerSession, List<McpRoot>>> handlers) {
		Objects.requireNonNull(handlers, "Handlers list must not be null");
		this.rootsChangeHandlers.addAll(handlers);
		return this;
	}

	/**
	 * Registers multiple consumers that will be notified when the list of roots
	 * changes using varargs. This method provides a convenient way to register
	 * multiple consumers inline.
	 *
	 * @param handlers The handlers to register. Must not be null.
	 * @return This builder instance for method chaining
	 * @throws IllegalArgumentException if consumers is null
	 * @see #rootsChangeHandlers(List)
	 */
	@SafeVarargs
	public final McpServer rootsChangeHandlers(BiConsumer<McpServerSession, List<McpRoot>>... handlers) {
		Objects.requireNonNull(handlers, "Handlers list must not be null");
		return this.rootsChangeHandlers(Arrays.asList(handlers));
	}

	/**
	 * sse endpoint
	 *
	 * @param request HttpRequest
	 * @return HttpResponse
	 */
	public HttpResponse sseEndpoint(HttpRequest request) {
		HttpResponse httpResponse = new HttpResponse(request);
		// 构造 sse
		HttpStream stream = httpResponse.startSse(request);
		// 响应包发送后，再发送 sse 回包
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
			JsonRpcResponse rpcResponse = handleIncomingRequest(session, (JsonRpcRequest) jsonRpcMessage);
			session.sendMessage(rpcResponse);
		} else if (jsonRpcMessage instanceof JsonRpcNotification) {
			JsonRpcNotification notification = (JsonRpcNotification) jsonRpcMessage;
			log.info("JsonRpcNotification:{}", notification);
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

	/**
	 * Handles an incoming JSON-RPC request by routing it to the appropriate handler.
	 *
	 * @param session McpServerSession
	 * @param request The incoming JSON-RPC request
	 * @return A Mono containing the JSON-RPC response
	 */
	private JsonRpcResponse handleIncomingRequest(McpServerSession session, JsonRpcRequest request) {
		String method = request.getMethod();
		if (McpSchema.METHOD_INITIALIZE.equals(method)) {
			McpInitializeRequest initializeRequest = JsonUtil.convertValue(request.getParams(), McpInitializeRequest.class);

			McpInitializeResult result = new McpInitializeResult();
			// 设置协议版本
			result.setProtocolVersion(initializeRequest.getProtocolVersion());

			// 服务信息
			result.setCapabilities(serverCapabilities);
			// 服务信息
			result.setServerInfo(serverInfo);

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

			McpListToolsResult toolsResult = new McpListToolsResult();

			List<McpTool> tools = new ArrayList<>();
			for (McpToolSpecification toolSpecification : this.tools) {
				tools.add(toolSpecification.getTool());
			}

			toolsResult.setTools(tools);
			jsonRpcResponse.setResult(toolsResult);
			return jsonRpcResponse;
		} else if (McpSchema.METHOD_TOOLS_CALL.equals(method)) {
			JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
			jsonRpcResponse.setJsonrpc(McpSchema.JSONRPC_VERSION);
			jsonRpcResponse.setId(request.getId());

			// 参数转换
			McpCallToolRequest callToolRequest = JsonUtil.convertValue(request.getParams(), McpCallToolRequest.class);
			String name = callToolRequest.getName();
			McpCallToolResult toolResult = null;
			for (McpToolSpecification toolSpecification : this.tools) {
				McpTool tool = toolSpecification.getTool();
				if (tool.getName().equals(name)) {
					Map<String, Object> toolArguments = getCallToolArguments(callToolRequest.getArguments());
					toolResult = toolSpecification.getCall().apply(session, toolArguments);
					break;
				}
			}

			if (toolResult == null) {
				throw new IllegalArgumentException("Cannot find tool with name " + name);
			}
			jsonRpcResponse.setResult(toolResult);
			return jsonRpcResponse;
		}
		return null;
	}

	/**
	 * 解码消息
	 *
	 * @param requestBody requestBody
	 * @return JsonRpcMessage
	 */
	@SuppressWarnings("unchecked")
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
	 * 解析 call tool 参数
	 *
	 * @param arguments arguments
	 * @return 参数 map
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> getCallToolArguments(Object arguments) {
		if (arguments == null) {
			return null;
		} else if (arguments instanceof Map) {
			return (Map<String, Object>) arguments;
		} else if (arguments instanceof String && StrUtil.isBlank((String) arguments)) {
			return null;
		} else {
			return JsonUtil.convertValue(arguments, Map.class);
		}
	}
}
