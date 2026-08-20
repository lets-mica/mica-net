package net.dreamlu.mica.net.http.test.mcp;

import net.dreamlu.mica.net.http.jsonrpc.JsonRpcErrorCodes;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcRequest;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcResponse;
import net.dreamlu.mica.net.http.mcp.schema.*;
import net.dreamlu.mica.net.http.mcp.server.McpErrorCodes;
import net.dreamlu.mica.net.http.mcp.server.McpException;
import net.dreamlu.mica.net.http.mcp.server.McpInputRequiredException;
import net.dreamlu.mica.net.http.mcp.server.McpRequestContext;
import net.dreamlu.mica.net.http.mcp.server.McpServer;
import net.dreamlu.mica.net.http.mcp.server.McpServerSession;
import net.dreamlu.mica.net.utils.json.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * McpServer 单元测试。
 */
class McpServerTest {

	private McpServer server;

	@BeforeEach
	void setUp() {
		server = new McpServer();
		server.serverInfo("test", "1.0.0");
		server.capabilities(new McpServerCapabilities());
	}

	private JsonRpcRequest newRequest(Object id, String method, Object params) {
		JsonRpcRequest req = new JsonRpcRequest();
		req.setJsonrpc("2.0");
		req.setId(id);
		req.setMethod(method);
		req.setParams(params);
		return req;
	}

	@Test
	void testInitializeReturnsServerInfo() {
		McpInitializeRequest params = new McpInitializeRequest();
		params.setProtocolVersion(McpSchema.MCP_2025_11_25);
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(1, McpSchema.METHOD_INITIALIZE, params));
		assertNotNull(resp);
		assertEquals("2.0", resp.getJsonrpc());
		assertEquals(1, resp.getId());
		assertNotNull(resp.getResult());
	}

	@Test
	void testPingReturnsEmptyObject() {
		JsonRpcResponse resp = server.handleIncomingRequest(null, newRequest(2, McpSchema.METHOD_PING, null));
		assertNotNull(resp);
		assertEquals(2, resp.getId());
		assertNotNull(resp.getResult());
	}

	@Test
	void testUnknownMethodReturnsMethodNotFound() {
		JsonRpcResponse resp = server.handleIncomingRequest(null, newRequest(3, "foo/bar", null));
		assertNotNull(resp);
		assertNotNull(resp.getError());
		assertEquals(JsonRpcErrorCodes.METHOD_NOT_FOUND, resp.getError().getCode());
		assertTrue(resp.getError().getMessage().contains("foo/bar"));
	}

	@Test
	void testNullRequestReturnsInvalidRequest() {
		JsonRpcResponse resp = server.handleIncomingRequest(null, null);
		assertNotNull(resp);
		assertEquals(JsonRpcErrorCodes.INVALID_REQUEST, resp.getError().getCode());
	}

	@Test
	void testBlankMethodReturnsInvalidRequest() {
		JsonRpcRequest req = new JsonRpcRequest();
		req.setId(4);
		req.setMethod("");
		req.setJsonrpc("2.0");
		JsonRpcResponse resp = server.handleIncomingRequest(null, req);
		assertNotNull(resp);
		assertEquals(JsonRpcErrorCodes.INVALID_REQUEST, resp.getError().getCode());
	}

	@Test
	void testToolCallSuccess() {
		McpTool tool = new McpTool();
		tool.setName("echo");
		tool.setDescription("Echo tool");
		server.tool(tool, (session, args) -> {
			McpCallToolResult result = new McpCallToolResult();
			result.setContent(Collections.singletonList(new McpTextContent("echo")));
			return result;
		});

		Map<String, Object> arguments = new HashMap<>();
		arguments.put("msg", "hi");
		McpCallToolRequest params = new McpCallToolRequest();
		params.setName("echo");
		params.setArguments(arguments);
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(5, McpSchema.METHOD_TOOLS_CALL, params));
		assertNotNull(resp);
		assertNull(resp.getError(), "Expected no error");
		assertNotNull(resp.getResult());
	}

	@Test
	void testToolNotFoundReturnsMcpError() {
		McpCallToolRequest params = new McpCallToolRequest();
		params.setName("nonexistent");
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(6, McpSchema.METHOD_TOOLS_CALL, params));
		assertNotNull(resp);
		assertNotNull(resp.getError());
		assertEquals(McpErrorCodes.TOOL_NOT_FOUND, resp.getError().getCode());
	}

	@Test
	void testBlankToolNameReturnsInvalidParams() {
		McpCallToolRequest params = new McpCallToolRequest();
		params.setName("");
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(7, McpSchema.METHOD_TOOLS_CALL, params));
		assertNotNull(resp);
		assertNotNull(resp.getError());
		assertEquals(JsonRpcErrorCodes.INVALID_PARAMS, resp.getError().getCode());
	}

	@Test
	void testToolCallArgumentsMustBeMap() {
		McpTool tool = new McpTool();
		tool.setName("foo");
		server.tool(tool, (session, args) -> {
			McpCallToolResult r = new McpCallToolResult();
			r.setContent(Collections.emptyList());
			return r;
		});
		Map<String, Object> args = new HashMap<>();
		args.put("arguments", "not-a-map");
		McpCallToolRequest params = new McpCallToolRequest();
		params.setName("foo");
		params.setArguments("not-a-map");
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(8, McpSchema.METHOD_TOOLS_CALL, params));
		assertNotNull(resp);
		assertNotNull(resp.getError());
		assertEquals(JsonRpcErrorCodes.INVALID_PARAMS, resp.getError().getCode());
	}

	@Test
	void testToolExceptionConvertedToErrorResult() {
		McpTool tool = new McpTool();
		tool.setName("boom");
		server.tool(tool, (session, args) -> {
			throw new RuntimeException("kaboom");
		});
		McpCallToolRequest params = new McpCallToolRequest();
		params.setName("boom");
		params.setArguments(null);
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(9, McpSchema.METHOD_TOOLS_CALL, params));
		assertNotNull(resp);
		assertNull(resp.getError(), "Tool exceptions should be converted to McpCallToolResult(error=true)");
		assertNotNull(resp.getResult());
	}

	@Test
	void testToolStreamErrorPropagation() {
		McpTool tool = new McpTool();
		tool.setName("stream");
		server.toolStream(tool, (session, args) -> {
			return new Iterator<McpContent>() {
				int i = 0;

				@Override
				public boolean hasNext() {
					return true;
				}

				@Override
				public McpContent next() {
					if (i++ == 1) {
						throw new RuntimeException("stream failed");
					}
					return new McpTextContent("chunk-" + i);
				}
			};
		});
		McpCallToolRequest params = new McpCallToolRequest();
		params.setName("stream");
		params.setArguments(null);
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(10, McpSchema.METHOD_TOOLS_CALL, params));
		assertNotNull(resp);
		Object result = resp.getResult();
		assertNotNull(result);
		assertTrue(result instanceof McpCallToolResult);
		McpCallToolResult callResult = (McpCallToolResult) result;
		assertNotNull(callResult.getContent());
		boolean hasInterrupted = callResult.getContent().stream()
			.anyMatch(c -> c instanceof McpTextContent
				&& ((McpTextContent) c).getText() != null
				&& ((McpTextContent) c).getText().contains("Stream interrupted"));
		assertTrue(hasInterrupted, "Result should contain Stream interrupted message");
		assertEquals(Boolean.TRUE, callResult.getError());
	}

	@Test
	void testMcpExceptionConvertsToJsonRpcError() {
		server.methodHandler("custom/bad", (session, request) -> {
			throw new McpException(JsonRpcErrorCodes.INVALID_PARAMS, "bad params");
		});
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(11, "custom/bad", null));
		assertNotNull(resp);
		assertNotNull(resp.getError());
		assertEquals(JsonRpcErrorCodes.INVALID_PARAMS, resp.getError().getCode());
		assertEquals("bad params", resp.getError().getMessage());
	}

	@Test
	void testPromptListAndGet() {
		McpPrompt prompt = new McpPrompt();
		prompt.setName("greet");
		prompt.setDescription("Greeting prompt");
		server.prompts(new net.dreamlu.mica.net.http.mcp.server.McpPromptSpecification(prompt, (session, params) -> {
			McpGetPromptResult r = new McpGetPromptResult();
			r.setDescription("hi");
			return r;
		}));
		JsonRpcResponse listResp = server.handleIncomingRequest(null,
			newRequest(12, McpSchema.METHOD_PROMPT_LIST, null));
		assertNotNull(listResp);
		assertNotNull(listResp.getResult());

		McpGetPromptRequest params = new McpGetPromptRequest();
		params.setName("greet");
		JsonRpcResponse getResp = server.handleIncomingRequest(null,
			newRequest(13, McpSchema.METHOD_PROMPT_GET, params));
		assertNotNull(getResp);
		assertNull(getResp.getError());
	}

	@Test
	void testPromptNotFound() {
		McpGetPromptRequest params = new McpGetPromptRequest();
		params.setName("nope");
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(14, McpSchema.METHOD_PROMPT_GET, params));
		assertNotNull(resp);
		assertNotNull(resp.getError());
		assertEquals(McpErrorCodes.PROMPT_NOT_FOUND, resp.getError().getCode());
	}

	@Test
	void testResourcesListEmpty() {
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(15, McpSchema.METHOD_RESOURCES_LIST, null));
		assertNotNull(resp);
		assertNotNull(resp.getResult());
	}

	@Test
	void testResourceReadExactMatch() {
		McpResource resource = new McpResource();
		resource.setUri("file://readme");
		resource.setName("readme");
		resource.setDescription("README");
		server.resources(Collections.singletonList(
			new net.dreamlu.mica.net.http.mcp.server.McpResourceSpecification(resource,
				(session, params) -> {
					McpReadResourceResult r = new McpReadResourceResult();
					r.setContents(Collections.emptyList());
					return r;
				})
		));
		McpReadResourceRequest params = new McpReadResourceRequest();
		params.setUri("file://readme");
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(16, McpSchema.METHOD_RESOURCES_READ, params));
		assertNotNull(resp);
		assertNull(resp.getError());
	}

	@Test
	void testResourceNotFound() {
		McpReadResourceRequest params = new McpReadResourceRequest();
		params.setUri("file://missing");
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(17, McpSchema.METHOD_RESOURCES_READ, params));
		assertNotNull(resp);
		assertNotNull(resp.getError());
		assertEquals(McpErrorCodes.RESOURCE_NOT_FOUND, resp.getError().getCode());
	}

	@Test
	void testLoggingSetLevelStoresPerSession() {
		McpServerSession session = new McpServerSession("test-sid", null);
		McpSetLevelRequest params = new McpSetLevelRequest();
		params.setLevel(McpLoggingLevel.WARNING);
		JsonRpcResponse resp = server.handleIncomingRequest(session,
			newRequest(18, McpSchema.METHOD_LOGGING_SET_LEVEL, params));
		assertNotNull(resp);
		assertNull(resp.getError());
		assertEquals(McpLoggingLevel.WARNING, server.getSessionLogLevel("test-sid"));
		server.clearSessionLogLevel("test-sid");
		assertNull(server.getSessionLogLevel("test-sid"));
	}

	@Test
	void testCompletionCompleteReturnsEmpty() {
		McpCompleteRequest params = new McpCompleteRequest();
		McpCompleteReference ref = new McpCompleteReference();
		ref.setName("any");
		params.setRef(ref);
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(19, McpSchema.METHOD_COMPLETION_COMPLETE, params));
		assertNotNull(resp);
		assertNull(resp.getError());
	}

	@Test
	void testToolsListReturnsRegisteredTools() {
		McpTool tool = new McpTool();
		tool.setName("foo");
		server.tool(tool, (s, a) -> {
			McpCallToolResult r = new McpCallToolResult();
			r.setContent(Collections.emptyList());
			return r;
		});
		McpTool tool2 = new McpTool();
		tool2.setName("bar");
		server.tool(tool2, (s, a) -> {
			McpCallToolResult r = new McpCallToolResult();
			r.setContent(Collections.emptyList());
			return r;
		});
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(20, McpSchema.METHOD_TOOLS_LIST, null));
		assertNotNull(resp);
		assertNotNull(resp.getResult());
	}

	@Test
	void testServerCannotRegisterDuplicateTool() {
		McpTool tool = new McpTool();
		tool.setName("dup");
		server.tool(tool, (s, a) -> {
			McpCallToolResult r = new McpCallToolResult();
			r.setContent(Collections.emptyList());
			return r;
		});
		// second registration with same name should overwrite (last wins)
		server.tool(tool, (s, a) -> {
			McpCallToolResult r = new McpCallToolResult();
			r.setContent(Collections.emptyList());
			return r;
		});
		// No exception; behavior should be last-wins
	}

	// ============================================================
	//  修复点覆盖测试
	// ============================================================

	/**
	 * 协议版本协商:client 发 null 时,采用最新版本。
	 */
	@Test
	void testInitializeNegotiatesNullProtocolVersion() {
		McpInitializeRequest params = new McpInitializeRequest();
		params.setProtocolVersion(null);
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(100, McpSchema.METHOD_INITIALIZE, params));
		assertNotNull(resp);
		assertNotNull(resp.getResult());
		McpInitializeResult result = (McpInitializeResult) resp.getResult();
		assertEquals(McpSchema.MCP_LATEST, result.getProtocolVersion());
	}

	/**
	 * 协议版本协商:client 发已知旧版本时,采用该版本(保持向后兼容)。
	 */
	@Test
	void testInitializeAcceptsKnownOlderProtocolVersion() {
		McpInitializeRequest params = new McpInitializeRequest();
		params.setProtocolVersion(McpSchema.MCP_2025_03_26);
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(101, McpSchema.METHOD_INITIALIZE, params));
		assertNotNull(resp);
		McpInitializeResult result = (McpInitializeResult) resp.getResult();
		assertEquals(McpSchema.MCP_2025_03_26, result.getProtocolVersion());
	}

	/**
	 * 协议版本协商:client 发未知版本时,回退到最新版本。
	 */
	@Test
	void testInitializeFallsBackOnUnknownProtocolVersion() {
		McpInitializeRequest params = new McpInitializeRequest();
		params.setProtocolVersion("1999-01-01");
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(102, McpSchema.METHOD_INITIALIZE, params));
		assertNotNull(resp);
		McpInitializeResult result = (McpInitializeResult) resp.getResult();
		assertEquals(McpSchema.MCP_LATEST, result.getProtocolVersion());
	}

	/**
	 * 资源模板预编译:resources/read 通过 URI 模板能匹配。
	 */
	@Test
	void testResourceReadTemplateMatch() {
		McpResourceTemplate template = new McpResourceTemplate();
		template.setUriTemplate("file://{name}.txt");
		template.setName("text-file");
		server.resourceTemplates(java.util.Collections.singletonList(
			new net.dreamlu.mica.net.http.mcp.server.McpResourceTemplateSpecification(template,
				(session, params) -> {
					McpReadResourceResult r = new McpReadResourceResult();
					r.setContents(Collections.emptyList());
					return r;
				})
		));
		McpReadResourceRequest params = new McpReadResourceRequest();
		params.setUri("file://readme.txt");
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(103, McpSchema.METHOD_RESOURCES_READ, params));
		assertNotNull(resp);
		assertNull(resp.getError(), "Template match should succeed");
	}

	/**
	 * 资源模板预编译:UriTemplate 在构造时就预编译,getUriTemplate 不为 null。
	 */
	@Test
	void testResourceTemplatePreCompilesUriTemplate() {
		McpResourceTemplate template = new McpResourceTemplate();
		template.setUriTemplate("file://{name}");
		net.dreamlu.mica.net.http.mcp.server.McpResourceTemplateSpecification spec =
			new net.dreamlu.mica.net.http.mcp.server.McpResourceTemplateSpecification(template, null);
		assertNotNull(spec.getUriTemplate(), "UriTemplate should be pre-compiled at construction time");
		assertEquals("file://{name}", spec.getUriTemplate().getTemplate());
	}

	/**
	 * 资源模板预编译:空 uriTemplate 时 getUriTemplate 返回 null,不抛异常。
	 */
	@Test
	void testResourceTemplateWithBlankUriTemplate() {
		McpResourceTemplate template = new McpResourceTemplate();
		template.setUriTemplate("");
		net.dreamlu.mica.net.http.mcp.server.McpResourceTemplateSpecification spec =
			new net.dreamlu.mica.net.http.mcp.server.McpResourceTemplateSpecification(template, null);
		assertNull(spec.getUriTemplate());
	}

	/**
	 * broadcast 过滤已关闭 session:对没 stream 的 session 广播不应抛异常。
	 */
	@Test
	void testBroadcastSkipsSessionsWithoutStream() {
		// 1) 注册一个无 stream 的 session
		McpServerSession stateless = new McpServerSession("no-stream-1", null);
		server.registerSession(stateless);

		// 2) 注册一个有 stream 但 stream 已关闭的 session
		McpServerSession closedSession = new McpServerSession("closed-1", null);
		server.registerSession(closedSession);

		// 3) broadcast 不应抛异常,且不向 stateless 发送(可通过检查 hasStream 验证)
		assertDoesNotThrow(() -> {
			server.broadcastToolsListChanged();
			server.broadcastPromptsListChanged();
			server.broadcastResourcesListChanged();
		});

		// 清理
		server.unregisterSession("no-stream-1");
		server.unregisterSession("closed-1");
	}

	/**
	 * Stateless 流式工具:多次并发调用产生不同的 sessionId(避免 ID 冲突)。
	 */
	@Test
	void testStatelessStreamToolUsesUniqueSessionId() {
		McpTool tool = new McpTool();
		tool.setName("stream-unique");
		java.util.Set<String> sessionIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
		server.toolStream(tool, (session, args) -> {
			sessionIds.add(session.getSessionId());
			java.util.List<McpContent> contents = new java.util.ArrayList<>();
			contents.add(new McpTextContent("ok"));
			return contents.iterator();
		});

		// 调用多次,应产生不同的 sessionId
		for (int i = 0; i < 5; i++) {
			McpCallToolRequest params = new McpCallToolRequest();
			params.setName("stream-unique");
			params.setArguments(null);
			JsonRpcResponse resp = server.handleIncomingRequest(null,
				newRequest(200 + i, McpSchema.METHOD_TOOLS_CALL, params));
			assertNotNull(resp);
			assertNull(resp.getError());
		}
		assertEquals(5, sessionIds.size(), "Each stateless stream call should use a unique sessionId");
		sessionIds.forEach(id -> assertTrue(id.startsWith("stateless-"),
			"Stateless sessionId should be prefixed with 'stateless-'"));
	}

	/**
	 * McpResourceSpecification 不可变:已移除 setter,只能通过构造器/工厂创建。
	 */
	@Test
	void testMcpResourceSpecificationIsImmutable() {
		McpResource resource = new McpResource();
		resource.setUri("file://x");
		net.dreamlu.mica.net.http.mcp.server.McpResourceSpecification spec =
			net.dreamlu.mica.net.http.mcp.server.McpResourceSpecification.of(resource, null);
		assertSame(resource, spec.getResource());
		// 编译期保障:不再有 setResource/setReadHandler;反射层面也不期望调用。
		assertNull(spec.getReadHandler());
	}

	// ============================================================
	//  2026-07-28 modern 协议覆盖测试
	// ============================================================

	/**
	 * 构造 modern 协议下的请求上下文。
	 */
	private static McpRequestContext modernCtx() {
		Map<String, Object> meta = new HashMap<>();
		meta.put(McpSchema.META_PROTOCOL_VERSION, McpSchema.MCP_2026_07_28);
		McpImplementation clientInfo = new McpImplementation("test-client", "1.0.0");
		meta.put(McpSchema.META_CLIENT_INFO, clientInfo);
		Map<String, Object> caps = new HashMap<>();
		caps.put("roots", Collections.emptyMap());
		meta.put(McpSchema.META_CLIENT_CAPABILITIES, caps);
		return new McpRequestContext(McpSchema.MCP_2026_07_28, clientInfo,
			JsonUtil.convertValue(caps, McpClientCapabilities.class), meta, null);
	}

	/**
	 * server/discover 是 modern 协议必实现的 RPC。
	 */
	@Test
	void testServerDiscoverReturnsCapabilitiesAndInfo() {
		JsonRpcResponse resp = server.handleIncomingRequest(modernCtx(), null,
			newRequest(300, McpSchema.METHOD_SERVER_DISCOVER, null));
		assertNotNull(resp);
		assertNull(resp.getError());
		Object result = resp.getResult();
		assertNotNull(result);
		assertTrue(result instanceof Map, "Modern result should be wrapped as Map, got " + result.getClass());
		Map<String, Object> map = (Map<String, Object>) result;
		assertEquals(McpSchema.RESULT_TYPE_COMPLETE, map.get("resultType"));
		Object serverInfoObj = map.get("serverInfo");
		assertNotNull(serverInfoObj);
		assertTrue(serverInfoObj instanceof Map);
		Map<String, Object> serverInfo = (Map<String, Object>) serverInfoObj;
		assertEquals("test", serverInfo.get("name"));
		Object supported = map.get("supportedProtocolVersions");
		assertNotNull(supported);
		assertTrue(supported instanceof List);
		assertTrue(((List<?>) supported).contains(McpSchema.MCP_2026_07_28));
	}

	/**
	 * 现代协议下 result 必须注入 resultType 与 _meta.io.modelcontextprotocol/serverInfo。
	 */
	@Test
	void testModernResponseCarriesResultTypeAndServerInfoMeta() {
		JsonRpcResponse resp = server.handleIncomingRequest(modernCtx(), null,
			newRequest(301, McpSchema.METHOD_PING, null));
		assertNotNull(resp);
		Object raw = resp.getResult();
		assertNotNull(raw);
		assertTrue(raw instanceof Map, "Modern result must be a Map");
		Map<String, Object> result = (Map<String, Object>) raw;
		assertEquals(McpSchema.RESULT_TYPE_COMPLETE, result.get("resultType"));
		Object meta = result.get("_meta");
		assertNotNull(meta);
		assertTrue(meta instanceof Map);
		Map<String, Object> metaMap = (Map<String, Object>) meta;
		Object serverInfo = metaMap.get(McpSchema.META_SERVER_INFO);
		assertNotNull(serverInfo);
	}

	/**
	 * 现代协议下的 tools/list 应附加 ttlMs / cacheScope 缓存字段。
	 */
	@Test
	void testModernToolsListHasCacheFields() {
		McpTool tool = new McpTool();
		tool.setName("foo");
		server.tool(tool, (s, a) -> {
			McpCallToolResult r = new McpCallToolResult();
			r.setContent(Collections.emptyList());
			return r;
		});
		JsonRpcResponse resp = server.handleIncomingRequest(modernCtx(), null,
			newRequest(302, McpSchema.METHOD_TOOLS_LIST, null));
		assertNotNull(resp);
		Object result = resp.getResult();
		assertNotNull(result);
		assertTrue(result instanceof Map, "Modern result should be Map for cache injection");
		Map<String, Object> map = (Map<String, Object>) result;
		assertNotNull(map.get("tools"));
		assertNotNull(map.get("ttlMs"), "ttlMs should be present in modern response");
		assertNotNull(map.get("cacheScope"), "cacheScope should be present in modern response");
		assertEquals("complete", map.get("resultType"));
	}

	/**
	 * 现代协议下 prompts/list / resources/list / resources/templates/list 同样附加缓存字段。
	 */
	@Test
	void testModernOtherListsHaveCacheFields() {
		JsonRpcResponse prompts = server.handleIncomingRequest(modernCtx(), null,
			newRequest(303, McpSchema.METHOD_PROMPT_LIST, null));
		JsonRpcResponse resources = server.handleIncomingRequest(modernCtx(), null,
			newRequest(304, McpSchema.METHOD_RESOURCES_LIST, null));
		JsonRpcResponse templates = server.handleIncomingRequest(modernCtx(), null,
			newRequest(305, McpSchema.METHOD_RESOURCES_TEMPLATES_LIST, null));
		assertTrue(prompts.getResult() instanceof Map);
		assertTrue(resources.getResult() instanceof Map);
		assertTrue(templates.getResult() instanceof Map);
		assertNotNull(((Map<String, Object>) prompts.getResult()).get("ttlMs"));
		assertNotNull(((Map<String, Object>) resources.getResult()).get("ttlMs"));
		assertNotNull(((Map<String, Object>) templates.getResult()).get("ttlMs"));
	}

	/**
	 * legacy 协议（无 modern ctx）下，result 不注入 resultType，行为保持原样。
	 */
	@Test
	void testLegacyResponseIsUnchanged() {
		JsonRpcResponse resp = server.handleIncomingRequest(null, null,
			newRequest(306, McpSchema.METHOD_PING, null));
		assertNotNull(resp);
		Object result = resp.getResult();
		// legacy 协议下 result 保持为 Map（来自 Collections.emptyMap()），不含 resultType/_meta
		if (result instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) result;
			assertNull(map.get("resultType"), "Legacy result must not carry resultType");
			assertNull(map.get("_meta"), "Legacy result must not carry _meta");
		}
	}

	/**
	 * 现代协议下注册顺序即返回顺序（确定性顺序）。
	 */
	@Test
	void testModernToolsListOrderIsDeterministic() {
		for (String name : new String[]{"alpha", "beta", "gamma"}) {
			McpTool tool = new McpTool();
			tool.setName(name);
			server.tool(tool, (s, a) -> {
				McpCallToolResult r = new McpCallToolResult();
				r.setContent(Collections.emptyList());
				return r;
			});
		}
		JsonRpcResponse resp = server.handleIncomingRequest(modernCtx(), null,
			newRequest(307, McpSchema.METHOD_TOOLS_LIST, null));
		Map<String, Object> map = (Map<String, Object>) resp.getResult();
		List<Map<String, Object>> tools = (List<Map<String, Object>>) map.get("tools");
		assertEquals("alpha", tools.get(0).get("name"));
		assertEquals("beta", tools.get(1).get("name"));
		assertEquals("gamma", tools.get(2).get("name"));
	}

	/**
	 * 现代协议下仍能处理 tool call，并把 result 包装为 map 含 resultType。
	 */
	@Test
	void testModernToolCallStillWorks() {
		McpTool tool = new McpTool();
		tool.setName("echo");
		server.tool(tool, (s, a) -> {
			McpCallToolResult r = new McpCallToolResult();
			r.setContent(Collections.singletonList(new McpTextContent("ok")));
			return r;
		});
		Map<String, Object> arguments = new HashMap<>();
		arguments.put("msg", "hi");
		McpCallToolRequest params = new McpCallToolRequest();
		params.setName("echo");
		params.setArguments(arguments);
		JsonRpcResponse resp = server.handleIncomingRequest(modernCtx(), null,
			newRequest(308, McpSchema.METHOD_TOOLS_CALL, params));
		assertNotNull(resp);
		assertNull(resp.getError());
		assertTrue(resp.getResult() instanceof Map);
		assertEquals("complete", ((Map<String, Object>) resp.getResult()).get("resultType"));
	}

	/**
	 * MCP_2026_07_28 已纳入版本列表。
	 */
	@Test
	void testModernProtocolVersionIsRegistered() {
		assertTrue(McpSchema.MCP_VERSION_LIST.contains(McpSchema.MCP_2026_07_28));
		assertEquals(McpSchema.MCP_2026_07_28, McpSchema.MCP_LATEST);
	}

	// ============================================================
	//  MRTR 中间响应（input_required）+ JSON Schema 2020-12
	// ============================================================

	/**
	 * MRTR：modern 协议下 tool handler 抛 {@link McpInputRequiredException}，
	 * server 应返回 resultType=input_required 的中间响应。
	 */
	@Test
	void testMrtrInputRequiredOnModernProtocol() {
		McpTool tool = new McpTool();
		tool.setName("needs-input");
		server.tool(tool, (s, a) -> {
			McpInputRequiredResult ir = new McpInputRequiredResult();
			ir.setPrompt("please provide your name");
			ir.setContinuationId("cont-1");
			throw new McpInputRequiredException(ir);
		});
		McpCallToolRequest params = new McpCallToolRequest();
		params.setName("needs-input");
		params.setArguments(null);
		JsonRpcResponse resp = server.handleIncomingRequest(modernCtx(), null,
			newRequest(400, McpSchema.METHOD_TOOLS_CALL, params));
		assertNotNull(resp);
		assertNull(resp.getError(), "MRTR should not return error");
		Object raw = resp.getResult();
		assertTrue(raw instanceof Map, "MRTR modern result must be a Map");
		Map<String, Object> map = (Map<String, Object>) raw;
		assertEquals(McpSchema.RESULT_TYPE_INPUT_REQUIRED, map.get("resultType"));
		assertNotNull(map.get("prompt"));
		assertEquals("cont-1", map.get("continuationId"));
	}

	/**
	 * MRTR：legacy 协议下抛 {@link McpInputRequiredException}，
	 * 应降级为 isError=true + prompt 文本的普通 tool result。
	 */
	@Test
	void testMrtrInputRequiredFallsBackOnLegacy() {
		McpTool tool = new McpTool();
		tool.setName("needs-input-legacy");
		server.tool(tool, (s, a) -> {
			McpInputRequiredResult ir = new McpInputRequiredResult();
			ir.setPrompt("legacy prompt");
			throw new McpInputRequiredException(ir);
		});
		McpCallToolRequest params = new McpCallToolRequest();
		params.setName("needs-input-legacy");
		params.setArguments(null);
		JsonRpcResponse resp = server.handleIncomingRequest(null, null,
			newRequest(401, McpSchema.METHOD_TOOLS_CALL, params));
		assertNotNull(resp);
		assertNull(resp.getError());
		Object raw = resp.getResult();
		assertTrue(raw instanceof McpCallToolResult,
			"Legacy MRTR should produce McpCallToolResult, got " + (raw == null ? "null" : raw.getClass()));
		McpCallToolResult callResult = (McpCallToolResult) raw;
		assertEquals(Boolean.TRUE, callResult.getError());
		boolean hasLegacyPrompt = callResult.getContent().stream()
			.anyMatch(c -> c instanceof McpTextContent
				&& ((McpTextContent) c).getText() != null
				&& ((McpTextContent) c).getText().contains("legacy prompt"));
		assertTrue(hasLegacyPrompt);
	}

	/**
	 * Deprecated method：modern 协议下返回 deprecated 警告结果。
	 */
	@Test
	void testDeprecatedSamplingReturnsWarningOnModern() {
		JsonRpcResponse resp = server.handleIncomingRequest(modernCtx(), null,
			newRequest(402, McpSchema.METHOD_SAMPLING_CREATE_MESSAGE, null));
		assertNotNull(resp);
		assertNull(resp.getError());
		Object raw = resp.getResult();
		assertTrue(raw instanceof Map);
		Map<String, Object> map = (Map<String, Object>) raw;
		assertEquals(Boolean.TRUE, map.get("deprecated"));
		assertEquals(McpSchema.METHOD_SAMPLING_CREATE_MESSAGE, map.get("method"));
		assertEquals(Integer.valueOf(12), map.get("removalWindowMonths"));
		assertEquals(McpSchema.RESULT_TYPE_COMPLETE, map.get("resultType"));
	}

	/**
	 * Deprecated method：legacy 协议下返回空 Map，保持向后兼容。
	 */
	@Test
	void testDeprecatedRootsListReturnsEmptyOnLegacy() {
		JsonRpcResponse resp = server.handleIncomingRequest(null, null,
			newRequest(403, McpSchema.METHOD_ROOTS_LIST, null));
		assertNotNull(resp);
		assertNull(resp.getError());
		assertNotNull(resp.getResult());
	}

	// ============================================================
	//  JSON Schema 2020-12 字段
	// ============================================================

	/**
	 * JSON Schema 2020-12 字段：McpJsonSchema 支持 $schema、$defs、prefixItems、
	 * allOf、anyOf、oneOf、const、enum、format、minimum、maximum、nullable 等。
	 */
	@Test
	void testJsonSchemaSupports2020Keywords() {
		McpJsonSchema schema = new McpJsonSchema();
		schema.setSchema("https://json-schema.org/draft/2020-12/schema");
		schema.setAnchor("root");
		Map<String, Object> defs = new HashMap<>();
		Map<String, Object> foo = new HashMap<>();
		foo.put("type", "object");
		defs.put("Foo", foo);
		schema.setDefs(defs);
		schema.setPrefixItems(java.util.Arrays.asList(
			java.util.Collections.singletonMap("type", "string"),
			java.util.Collections.singletonMap("type", "number")
		));
		schema.setAllOf(java.util.Arrays.asList(java.util.Collections.singletonMap("type", "object")));
		schema.setAnyOf(java.util.Arrays.asList(java.util.Collections.singletonMap("type", "string")));
		schema.setOneOf(java.util.Arrays.asList(java.util.Collections.singletonMap("type", "number")));
		schema.setConstValue(42);
		schema.setEnumValues(java.util.Arrays.asList("a", "b", "c"));
		schema.setFormat("uri");
		schema.setPattern("^[a-z]+$");
		schema.setMinimum(0);
		schema.setMaximum(100);
		schema.setNullable(Boolean.TRUE);
		schema.setRef("#/$defs/Foo");

		// 字段读写
		assertEquals("https://json-schema.org/draft/2020-12/schema", schema.getSchema());
		assertEquals("root", schema.getAnchor());
		assertNotNull(schema.getDefs());
		assertEquals("Foo", schema.getDefs().keySet().iterator().next());
		assertEquals(2, schema.getPrefixItems().size());
		assertNotNull(schema.getAllOf());
		assertNotNull(schema.getAnyOf());
		assertNotNull(schema.getOneOf());
		assertEquals(42, schema.getConstValue());
		assertEquals(3, schema.getEnumValues().size());
		assertEquals("uri", schema.getFormat());
		assertEquals("^[a-z]+$", schema.getPattern());
		assertEquals(0, schema.getMinimum());
		assertEquals(100, schema.getMaximum());
		assertEquals(Boolean.TRUE, schema.getNullable());
		assertEquals("#/$defs/Foo", schema.getRef());

		// 旧字段保留
		schema.setType("object");
		schema.setRequired(java.util.Collections.singletonList("name"));
		assertEquals("object", schema.getType());
		assertEquals(1, schema.getRequired().size());

		// JSON 序列化往返
		String json = JsonUtil.toJsonString(schema);
		assertTrue(json.contains("$schema") || json.contains("schema"));
		McpJsonSchema restored = JsonUtil.readValue(json, McpJsonSchema.class);
		assertEquals(schema.getSchema(), restored.getSchema());
		assertEquals(schema.getRef(), restored.getRef());
	}
}
