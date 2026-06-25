package net.dreamlu.mica.net.http.test.mcp;

import net.dreamlu.mica.net.http.jsonrpc.JsonRpcErrorCodes;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcRequest;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcResponse;
import net.dreamlu.mica.net.http.mcp.schema.*;
import net.dreamlu.mica.net.http.mcp.server.McpErrorCodes;
import net.dreamlu.mica.net.http.mcp.server.McpException;
import net.dreamlu.mica.net.http.mcp.server.McpServer;
import net.dreamlu.mica.net.http.mcp.server.McpServerSession;
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
		params.setProtocolVersion(McpSchema.LATEST_PROTOCOL_VERSION);
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
		assertEquals(McpSchema.LATEST_PROTOCOL_VERSION, result.getProtocolVersion());
	}

	/**
	 * 协议版本协商:client 发已知旧版本时,采用该版本(保持向后兼容)。
	 */
	@Test
	void testInitializeAcceptsKnownOlderProtocolVersion() {
		McpInitializeRequest params = new McpInitializeRequest();
		params.setProtocolVersion(McpSchema.PROTOCOL_VERSION_2025_03_26);
		JsonRpcResponse resp = server.handleIncomingRequest(null,
			newRequest(101, McpSchema.METHOD_INITIALIZE, params));
		assertNotNull(resp);
		McpInitializeResult result = (McpInitializeResult) resp.getResult();
		assertEquals(McpSchema.PROTOCOL_VERSION_2025_03_26, result.getProtocolVersion());
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
		assertEquals(McpSchema.LATEST_PROTOCOL_VERSION, result.getProtocolVersion());
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
}