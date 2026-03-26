package org.tio.http.mcp.server;

import org.tio.http.common.stream.HttpStream;
import org.tio.http.jsonrpc.JsonRpcMessage;
import org.tio.http.mcp.schema.McpContent;
import org.tio.utils.json.JsonUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * mcp 服务端 session
 *
 * @author L.cm
 */
public class McpServerSession {
	/**
	 * Event type for JSON-RPC messages sent through the SSE connection.
	 */
	public static final String MESSAGE_EVENT_TYPE = "message";
	private final String sessionId;
	private final HttpStream httpStream;

	public McpServerSession(String sessionId, HttpStream httpStream) {
		this.sessionId = sessionId;
		this.httpStream = httpStream;
	}

	/**
	 * 发送心跳
	 */
	public void sendHeartbeat() {
		httpStream.send(null, null, "heartbeat");
	}

	/**
	 * 发送消息
	 *
	 * @param message JsonRpcMessage
	 */
	public void sendMessage(JsonRpcMessage message) {
		httpStream.send(MESSAGE_EVENT_TYPE, JsonUtil.toJsonString(message));
	}

	/**
	 * 发送单个 content chunk（SSE message 事件）
	 *
	 * @param content McpContent
	 */
	public void sendChunk(McpContent content) {
		httpStream.send(MESSAGE_EVENT_TYPE, JsonUtil.toJsonString(content));
	}

	/**
	 * 发送多个 content chunk
	 *
	 * @param contents Iterator of McpContent
	 */
	public void sendChunks(Iterator<McpContent> contents) {
		while (contents.hasNext()) {
			sendChunk(contents.next());
		}
	}

	/**
	 * 流式调用工具并在过程中推送结果
	 *
	 * @param toolSpec     tool specification
	 * @param args         调用参数
	 * @param returnDirect 是否直接返回（true=立即返回第一条，false=收集全部）
	 * @return McpCallToolResult
	 */
	public org.tio.http.mcp.schema.McpCallToolResult callToolStream(
			McpToolSpecification toolSpec,
			java.util.Map<String, Object> args,
			boolean returnDirect) {
		Iterator<McpContent> contentIter = toolSpec.callStream(this, args);
		org.tio.http.mcp.schema.McpCallToolResult result = new org.tio.http.mcp.schema.McpCallToolResult();
		if (returnDirect) {
			// returnDirect=true：立即返回前端，不缓存
			if (contentIter.hasNext()) {
				List<McpContent> contents = new ArrayList<>();
				contents.add(contentIter.next());
				result.setContent(contents);
			}
			return result;
		}
		// returnDirect=false：收集所有结果
		List<McpContent> contents = new ArrayList<>();
		while (contentIter.hasNext()) {
			contents.add(contentIter.next());
		}
		result.setContent(contents);
		return result;
	}

	/**
	 * 关闭
	 */
	public void close() {
		httpStream.close();
	}

	@Override
	public String toString() {
		return "McpServerSession{" +
			"sessionId='" + sessionId + '\'' +
			'}';
	}
}
