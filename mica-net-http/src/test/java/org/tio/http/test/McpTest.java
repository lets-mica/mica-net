package org.tio.http.test;

import org.tio.http.mcp.schema.*;
import org.tio.http.mcp.server.McpServer;
import org.tio.http.mcp.server.McpServerSession;
import org.tio.http.server.HttpServerStarter;
import org.tio.utils.json.Jackson2JsonAdapter;
import org.tio.utils.json.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * http 测试
 *
 * @author L.cm
 */
public class McpTest {

	public static void main(String[] args) throws IOException {
		// 测试 json 工具
		JsonUtil.getJsonAdapter(new Jackson2JsonAdapter());
//		JsonUtil.getJsonAdapter(new Jackson3JsonAdapter());
//		JsonUtil.getJsonAdapter(new FastJson2JsonAdapter());
//		JsonUtil.getJsonAdapter(new FastJson1JsonAdapter());
//		JsonUtil.getJsonAdapter(new GsonJsonAdapter());
//		JsonUtil.getJsonAdapter(new HuToolJsonAdapter());
//		JsonUtil.getJsonAdapter(new Snack3JsonAdapter());
		// mcp 官方文档地址：https://modelcontextprotocol.io/specification/draft/server/tools
		// 启动 mcp 服务
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

		// 注册传输层
		mcpServer.useSseTransport();
		mcpServer.useStreamableTransport();

		// 流式工具示例：逐步返回数据
		McpTool streamTool = new McpTool();
		streamTool.setName("streamData");
		streamTool.setDescription("流式返回数据");
		streamTool.setReturnDirect(false);

		McpJsonSchema streamInputSchema = new McpJsonSchema();
		streamInputSchema.setType("object");
		streamInputSchema.setProperties(new HashMap<>());
		streamInputSchema.setRequired(new ArrayList<>());
		streamTool.setInputSchema(streamInputSchema);

		mcpServer.toolStream(streamTool, (McpServerSession session, Map<String, Object> params) -> {
			// 返回一个 Iterator，遍历过程中主动推送 chunk
			return new Iterator<McpContent>() {
				private int index = 0;
				private final int total = 5;

				@Override
				public boolean hasNext() {
					return index < total;
				}

				@Override
				public McpContent next() {
					String data = "chunk_" + index;
					// 主动推送到客户端
					McpTextContent chunk = new McpTextContent(data);
					session.sendChunk(chunk);
					index++;
					return chunk;
				}
			};
		});

		TestMcpHandler mcpHandler = new TestMcpHandler(mcpServer);
		HttpServerStarter httpServerStarter = new HttpServerStarter(18083, mcpHandler);
		httpServerStarter.start();
	}

}
