package org.tio.http.mcp.server;

import org.tio.http.mcp.schema.McpCallToolResult;
import org.tio.http.mcp.schema.McpContent;
import org.tio.http.mcp.schema.McpTool;

import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * mcp tool 定义
 *
 * @author L.cm
 */
public class McpToolSpecification {
	private final McpTool tool;
	/**
	 * 同步调用
	 */
	private final BiFunction<McpServerSession, Map<String, Object>, McpCallToolResult> call;
	/**
	 * 流式调用（可选）
	 */
	private final BiFunction<McpServerSession, Map<String, Object>, Iterator<McpContent>> streamCall;

	/**
	 * 私有构造函数，使用静态工厂方法创建
	 */
	private McpToolSpecification(McpTool tool,
			BiFunction<McpServerSession, Map<String, Object>, McpCallToolResult> call,
			BiFunction<McpServerSession, Map<String, Object>, Iterator<McpContent>> streamCall) {
		this.tool = tool;
		this.call = call;
		this.streamCall = streamCall;
	}

	/**
	 * 创建同步调用的 Tool Specification
	 */
	public static McpToolSpecification of(McpTool tool,
			BiFunction<McpServerSession, Map<String, Object>, McpCallToolResult> call) {
		return new McpToolSpecification(tool, call, null);
	}

	/**
	 * 创建流式调用的 Tool Specification
	 */
	public static McpToolSpecification ofStream(McpTool tool,
			BiFunction<McpServerSession, Map<String, Object>, Iterator<McpContent>> streamCall) {
		return new McpToolSpecification(tool, null, streamCall);
	}

	public McpTool getTool() {
		return tool;
	}

	/**
	 * 是否为流式调用
	 */
	public boolean isStream() {
		return streamCall != null;
	}

	/**
	 * 同步调用
	 */
	public BiFunction<McpServerSession, Map<String, Object>, McpCallToolResult> getCall() {
		return call;
	}

	/**
	 * 流式调用入口
	 */
	public Iterator<McpContent> callStream(McpServerSession session, Map<String, Object> args) {
		if (streamCall == null) {
			throw new UnsupportedOperationException("This tool does not support streaming");
		}
		return streamCall.apply(session, args);
	}
}
