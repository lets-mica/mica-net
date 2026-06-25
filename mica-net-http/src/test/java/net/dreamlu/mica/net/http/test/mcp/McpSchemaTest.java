package net.dreamlu.mica.net.http.test.mcp;

import net.dreamlu.mica.net.http.mcp.schema.*;
import net.dreamlu.mica.net.utils.json.JsonUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * McpSchema 序列化 / 反序列化测试。
 */
class McpSchemaTest {

	@Test
	void testLatestProtocolVersionIsCurrent() {
		assertNotNull(McpSchema.LATEST_PROTOCOL_VERSION);
		assertFalse(McpSchema.LATEST_PROTOCOL_VERSION.isEmpty());
	}

	@Test
	void testMcpToolAnnotationsRoundTrip() {
		McpTool tool = new McpTool();
		tool.setName("foo");
		tool.setDescription("foo tool");
		McpAnnotations annotations = new McpAnnotations();
		annotations.setTitle("Foo");
		annotations.setReadOnlyHint(true);
		annotations.setDestructiveHint(false);
		tool.setAnnotations(annotations);

		String json = JsonUtil.toJsonString(tool);
		assertTrue(json.contains("\"annotations\""));
		McpTool parsed = JsonUtil.readValue(json, McpTool.class);
		assertEquals("foo", parsed.getName());
		assertNotNull(parsed.getAnnotations());
		assertEquals(Boolean.TRUE, parsed.getAnnotations().getReadOnlyHint());
	}

	@Test
	void testMcpResourceAnnotationsRoundTrip() {
		McpResource resource = new McpResource();
		resource.setUri("file://test");
		resource.setName("test");
		McpAnnotations a = new McpAnnotations();
		a.setAudience(Arrays.asList(McpRole.user, McpRole.assistant));
		resource.setAnnotations(a);

		String json = JsonUtil.toJsonString(resource);
		McpResource parsed = JsonUtil.readValue(json, McpResource.class);
		assertNotNull(parsed.getAnnotations());
		assertEquals(2, parsed.getAnnotations().getAudience().size());
	}

	@Test
	void testMcpLoggingLevelOrdinal() {
		assertTrue(McpLoggingLevel.DEBUG.ordinal() < McpLoggingLevel.INFO.ordinal());
		assertTrue(McpLoggingLevel.INFO.ordinal() < McpLoggingLevel.WARNING.ordinal());
		assertTrue(McpLoggingLevel.WARNING.ordinal() < McpLoggingLevel.ERROR.ordinal());
		assertTrue(McpLoggingLevel.ERROR.ordinal() < McpLoggingLevel.EMERGENCY.ordinal());
	}

	@Test
	void testMcpProgressNotificationRoundTrip() {
		McpProgressNotification n = new McpProgressNotification();
		n.setProgressToken("token-1");
		n.setProgress(0.5);
		n.setTotal(1.0);
		n.setMessage("processing");

		String json = JsonUtil.toJsonString(n);
		McpProgressNotification parsed = JsonUtil.readValue(json, McpProgressNotification.class);
		assertEquals("token-1", parsed.getProgressToken());
		assertEquals(0.5, parsed.getProgress());
		assertEquals(1.0, parsed.getTotal());
		assertEquals("processing", parsed.getMessage());
	}

	@Test
	void testMcpCancelledNotificationRoundTrip() {
		McpCancelledNotification n = new McpCancelledNotification();
		n.setRequestId(42);
		n.setReason("user cancelled");
		String json = JsonUtil.toJsonString(n);
		McpCancelledNotification parsed = JsonUtil.readValue(json, McpCancelledNotification.class);
		assertEquals(42, parsed.getRequestId());
		assertEquals("user cancelled", parsed.getReason());
	}

	@Test
	void testMcpCompleteResultRoundTrip() {
		McpCompleteResult r = new McpCompleteResult();
		r.setValues(Arrays.asList("alpha", "beta"));
		r.setTotal(2);
		r.setHasMore(false);
		String json = JsonUtil.toJsonString(r);
		McpCompleteResult parsed = JsonUtil.readValue(json, McpCompleteResult.class);
		assertEquals(2, parsed.getValues().size());
		assertEquals(Integer.valueOf(2), parsed.getTotal());
		assertEquals(Boolean.FALSE, parsed.getHasMore());
	}

	@Test
	void testMcpServerCapabilitiesCompletions() {
		McpServerCapabilities caps = new McpServerCapabilities();
		caps.setCompletions(new McpCompletionCapabilities());
		String json = JsonUtil.toJsonString(caps);
		assertTrue(json.contains("completions"));
		McpServerCapabilities parsed = JsonUtil.readValue(json, McpServerCapabilities.class);
		assertNotNull(parsed.getCompletions());
	}

	@Test
	void testMcpClientCapabilitiesSamplingAndRoots() {
		McpClientCapabilities caps = new McpClientCapabilities();
		caps.setSampling(new McpSamplingCapabilities());
		McpRootsCapabilities roots = new McpRootsCapabilities();
		roots.setListChanged(true);
		caps.setRoots(roots);
		Map<String, Object> experimental = new HashMap<>();
		experimental.put("custom", true);
		caps.setExperimental(experimental);

		String json = JsonUtil.toJsonString(caps);
		McpClientCapabilities parsed = JsonUtil.readValue(json, McpClientCapabilities.class);
		assertNotNull(parsed.getSampling());
		assertNotNull(parsed.getRoots());
		assertEquals(Boolean.TRUE, parsed.getRoots().getListChanged());
		assertNotNull(parsed.getExperimental());
	}

	@Test
	void testMcpSamplingRoundTrip() {
		McpSampling sampling = new McpSampling();
		sampling.setPrompt("Hello");
		sampling.setModelHint("gpt-4");
		sampling.setMaxTokens(100);
		sampling.setTemperature(0.7);
		String json = JsonUtil.toJsonString(sampling);
		McpSampling parsed = JsonUtil.readValue(json, McpSampling.class);
		assertEquals("Hello", parsed.getPrompt());
		assertEquals("gpt-4", parsed.getModelHint());
		assertEquals(Integer.valueOf(100), parsed.getMaxTokens());
		assertEquals(Double.valueOf(0.7), parsed.getTemperature());
	}

	@Test
	void testAllMethodConstantsPresent() {
		assertEquals("initialize", McpSchema.METHOD_INITIALIZE);
		assertEquals("ping", McpSchema.METHOD_PING);
		assertEquals("tools/list", McpSchema.METHOD_TOOLS_LIST);
		assertEquals("tools/call", McpSchema.METHOD_TOOLS_CALL);
		assertEquals("resources/list", McpSchema.METHOD_RESOURCES_LIST);
		assertEquals("resources/read", McpSchema.METHOD_RESOURCES_READ);
		assertEquals("prompts/list", McpSchema.METHOD_PROMPT_LIST);
		assertEquals("prompts/get", McpSchema.METHOD_PROMPT_GET);
		assertEquals("completion/complete", McpSchema.METHOD_COMPLETION_COMPLETE);
		assertEquals("notifications/progress", McpSchema.METHOD_NOTIFICATION_PROGRESS);
		assertEquals("notifications/cancelled", McpSchema.METHOD_NOTIFICATION_CANCELLED);
	}
}