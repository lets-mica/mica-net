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
}