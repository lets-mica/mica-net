package net.dreamlu.mica.net.http.test.mcp;

import net.dreamlu.mica.net.http.mcp.server.McpServer;
import net.dreamlu.mica.net.http.mcp.server.transport.SseTransport;
import net.dreamlu.mica.net.http.mcp.server.transport.StreamableHttpTransport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * MCP transport 默认端点与路由分发测试。
 *
 * <p>保证 SSE 默认端点不会与 Streamable HTTP 的默认端点冲突，
 * 以便同时启用两个 transport 时路由可正确分发。</p>
 */
class McpTransportTest {

	@Test
	void testSseTransportDefaultEndpoints() {
		SseTransport transport = new SseTransport(new McpServer());
		assertEquals("/mcp/sse", transport.getSseEndpoint());
		assertEquals("/mcp/sse/message", transport.getMessageEndpoint());
		assertEquals("sse", transport.getType());
	}

	@Test
	void testStreamableHttpTransportDefaultEndpoint() {
		StreamableHttpTransport transport = new StreamableHttpTransport(new McpServer());
		assertEquals("/mcp", transport.getEndpoint());
		assertEquals("streamable-http", transport.getType());
	}

	/**
	 * SSE 与 Streamable 默认端点必须不同，避免同时启用时路由冲突。
	 */
	@Test
	void testSseAndStreamableEndpointsDoNotConflict() {
		SseTransport sse = new SseTransport(new McpServer());
		StreamableHttpTransport streamable = new StreamableHttpTransport(new McpServer());
		// /mcp vs /mcp/sse 不重叠
		assertNotEquals(sse.getSseEndpoint(), streamable.getEndpoint());
		assertNotEquals(sse.getMessageEndpoint(), streamable.getEndpoint());
	}
}
