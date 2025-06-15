package org.tio.http.test;

import org.tio.http.common.HttpConfig;
import org.tio.http.mcp.schema.*;
import org.tio.http.mcp.server.McpServer;
import org.tio.http.server.HttpServerStarter;
import org.tio.utils.json.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * http 测试
 *
 *
 * @author L.cm
 */
public class McpTest {

	public static void main(String[] args) throws IOException {
		// 测试 json 工具
		JsonUtil.getJsonAdapter(new JacksonJsonAdapter());
//		JsonUtil.getJsonAdapter(new FastJson2JsonAdapter());
//		JsonUtil.getJsonAdapter(new FastJson1JsonAdapter());
//		JsonUtil.getJsonAdapter(new GsonJsonAdapter());
//		JsonUtil.getJsonAdapter(new HuToolJsonAdapter());
//		JsonUtil.getJsonAdapter(new Snack3JsonAdapter());
		// mcp 官方文档地址：https://modelcontextprotocol.io/specification/draft/server/tools
		// 启动 mcp 服务
		HttpConfig httpConfig = new HttpConfig();

		McpServer mcpServer = new McpServer();

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
		mcpServer.capabilities(serverCapabilities);

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

		mcpServer.tool(mcpTool, (mcpServerExchange, requestMap) -> {
			McpCallToolResult toolResult = new McpCallToolResult();

			Map<String, Object> json = new HashMap<>();
			json.put("status", "123123");

			McpTextContent content = new McpTextContent(JsonUtil.toJsonString(json));

			toolResult.setContent(Collections.singletonList(content));
			toolResult.setStructuredContent(json);

			return toolResult;
		});

		TestMcpHandler mcpHandler = new TestMcpHandler(mcpServer);
		HttpServerStarter httpServerStarter = new HttpServerStarter(httpConfig, mcpHandler);
		httpServerStarter.start(null, 8080);
	}

}
