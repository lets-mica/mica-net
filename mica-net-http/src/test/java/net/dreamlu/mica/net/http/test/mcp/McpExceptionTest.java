package net.dreamlu.mica.net.http.test.mcp;

import net.dreamlu.mica.net.http.jsonrpc.JsonRpcError;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcErrorCodes;
import net.dreamlu.mica.net.http.mcp.server.McpErrorCodes;
import net.dreamlu.mica.net.http.mcp.server.McpException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * McpException + McpErrorCodes 单元测试。
 */
class McpExceptionTest {

	@Test
	void testSimpleConstruction() {
		McpException e = new McpException(JsonRpcErrorCodes.INVALID_PARAMS, "bad");
		assertEquals(JsonRpcErrorCodes.INVALID_PARAMS, e.getCode());
		assertEquals("bad", e.getMessage());
		assertNull(e.getData());
		assertNull(e.getCause());
	}

	@Test
	void testConstructionWithData() {
		Map<String, Object> data = new HashMap<>();
		data.put("field", "name");
		McpException e = new McpException(JsonRpcErrorCodes.INVALID_PARAMS, "bad", data);
		assertEquals(JsonRpcErrorCodes.INVALID_PARAMS, e.getCode());
		assertSame(data, e.getData());
	}

	@Test
	void testConstructionWithCause() {
		Throwable cause = new RuntimeException("inner");
		McpException e = new McpException(JsonRpcErrorCodes.INTERNAL_ERROR, "boom", cause);
		assertEquals(JsonRpcErrorCodes.INTERNAL_ERROR, e.getCode());
		assertSame(cause, e.getCause());
	}

	@Test
	void testConstructionWithDataAndCause() {
		Throwable cause = new RuntimeException("inner");
		Map<String, Object> data = new HashMap<>();
		data.put("k", "v");
		McpException e = new McpException(JsonRpcErrorCodes.INTERNAL_ERROR, "boom", data, cause);
		assertEquals(JsonRpcErrorCodes.INTERNAL_ERROR, e.getCode());
		assertSame(data, e.getData());
		assertSame(cause, e.getCause());
	}

	@Test
	void testToJsonRpcErrorWithoutData() {
		McpException e = new McpException(McpErrorCodes.RESOURCE_NOT_FOUND, "missing");
		JsonRpcError err = e.toJsonRpcError();
		assertEquals(McpErrorCodes.RESOURCE_NOT_FOUND, err.getCode());
		assertEquals("missing", err.getMessage());
		assertNull(err.getData());
	}

	@Test
	void testToJsonRpcErrorWithData() {
		Map<String, Object> data = new HashMap<>();
		data.put("uri", "file://x");
		McpException e = new McpException(McpErrorCodes.RESOURCE_NOT_FOUND, "missing", data);
		JsonRpcError err = e.toJsonRpcError();
		assertEquals(McpErrorCodes.RESOURCE_NOT_FOUND, err.getCode());
		assertEquals("missing", err.getMessage());
		assertSame(data, err.getData());
	}

	@Test
	void testErrorCodesAreInValidRange() {
		// Standard JSON-RPC error codes are -32700 to -32603
		assertTrue(McpErrorCodes.RESOURCE_NOT_FOUND < -32000);
		assertTrue(McpErrorCodes.RESOURCE_NOT_FOUND >= -32099);
		assertTrue(McpErrorCodes.TOOL_NOT_FOUND < -32000);
		assertTrue(McpErrorCodes.TOOL_NOT_FOUND >= -32099);
		assertTrue(McpErrorCodes.PROMPT_NOT_FOUND < -32000);
		assertTrue(McpErrorCodes.PROMPT_NOT_FOUND >= -32099);
	}
}