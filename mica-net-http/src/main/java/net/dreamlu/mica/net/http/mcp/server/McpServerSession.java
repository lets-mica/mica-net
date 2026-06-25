package net.dreamlu.mica.net.http.mcp.server;

import net.dreamlu.mica.net.http.common.stream.HttpStream;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcMessage;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcNotification;
import net.dreamlu.mica.net.http.mcp.schema.McpCallToolResult;
import net.dreamlu.mica.net.http.mcp.schema.McpContent;
import net.dreamlu.mica.net.http.mcp.schema.McpSchema;
import net.dreamlu.mica.net.http.mcp.schema.McpTextContent;
import net.dreamlu.mica.net.utils.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * mcp 服务端 session
 *
 * <p>在 Streamable HTTP 模式下，{@link #httpStream} 可能为 null
 * （纯无状态的 POST 请求处理）。</p>
 *
 * @author L.cm
 */
public class McpServerSession {
	private static final Logger log = LoggerFactory.getLogger(McpServerSession.class);

	/**
	 * Event type for JSON-RPC messages sent through the SSE connection.
	 */
	public static final String MESSAGE_EVENT_TYPE = "message";
	/**
	 * Event type for keep-alive heartbeats.
	 */
	public static final String HEARTBEAT_EVENT_TYPE = "heartbeat";

	private final String sessionId;
	/**
	 * 可为 null，表示 stateless 模式（无 SSE 输出通道）。
	 */
	private final HttpStream httpStream;
	private final AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis());

	public McpServerSession(String sessionId, HttpStream httpStream) {
		this.sessionId = sessionId;
		this.httpStream = httpStream;
	}

	public String getSessionId() {
		return sessionId;
	}

	/**
	 * 获取底层的 HttpStream，可能为 null。
	 *
	 * @return HttpStream or null
	 */
	public HttpStream getHttpStream() {
		return httpStream;
	}

	/**
	 * 是否拥有可用（未关闭）的输出流。
	 *
	 * @return true 当且仅当 httpStream 非 null 且未关闭
	 */
	public boolean hasStream() {
		return httpStream != null && !httpStream.isClosed();
	}

	/**
	 * 获取最后一次访问时间（毫秒）。
	 *
	 * @return 最后访问时间戳
	 */
	public long getLastAccessTime() {
		return lastAccessTime.get();
	}

	/**
	 * 刷新最后访问时间，用于会话清理判定。
	 */
	public void touch() {
		lastAccessTime.set(System.currentTimeMillis());
	}

	/**
	 * 发送心跳（仅当拥有 stream 时生效）。
	 */
	public void sendHeartbeat() {
		if (httpStream != null) {
			httpStream.send(null, null, HEARTBEAT_EVENT_TYPE);
		}
	}

	/**
	 * 发送 JSON-RPC 消息。
	 *
	 * <p>无 stream 时静默忽略（stateless 模式）。</p>
	 *
	 * @param message JsonRpcMessage
	 */
	public void sendMessage(JsonRpcMessage message) {
		touch();
		if (httpStream == null) {
			log.debug("Skip sendMessage: session {} has no stream", sessionId);
			return;
		}
		httpStream.send(MESSAGE_EVENT_TYPE, JsonUtil.toJsonString(message));
	}

	/**
	 * 发送 JSON-RPC notification（无 id 的消息）。
	 *
	 * <p>无 stream 时静默忽略。</p>
	 *
	 * @param method method 名
	 * @param params params
	 */
	public void sendNotification(String method, Map<String, Object> params) {
		touch();
		if (httpStream == null) {
			log.debug("Skip sendNotification: session {} has no stream, method={}", sessionId, method);
			return;
		}
		JsonRpcNotification notification = new JsonRpcNotification();
		notification.setJsonrpc(McpSchema.JSONRPC_VERSION);
		notification.setMethod(method);
		notification.setParams(params);
		httpStream.send(MESSAGE_EVENT_TYPE, JsonUtil.toJsonString(notification));
	}

	/**
	 * 发送单个 content chunk（SSE message 事件）。
	 *
	 * @param content McpContent
	 */
	public void sendChunk(McpContent content) {
		touch();
		if (httpStream == null) {
			return;
		}
		httpStream.send(MESSAGE_EVENT_TYPE, JsonUtil.toJsonString(content));
	}

	/**
	 * 发送多个 content chunk。
	 *
	 * @param contents Iterator of McpContent
	 */
	public void sendChunks(Iterator<McpContent> contents) {
		while (contents.hasNext()) {
			sendChunk(contents.next());
		}
	}

	/**
	 * 流式调用工具并在过程中推送结果。
	 *
	 * <p>如果 iterator 在 {@code hasNext}/{@code next} 时抛出异常，
	 * 会捕获并以 {@code isError=true} + TextContent 形式附加到结果末尾。</p>
	 *
	 * @param toolSpec     tool specification
	 * @param args         调用参数
	 * @param returnDirect 是否直接返回（true=仅返回第一条，false=收集全部）
	 * @return McpCallToolResult
	 */
	public McpCallToolResult callToolStream(
		McpToolSpecification toolSpec,
		java.util.Map<String, Object> args,
		boolean returnDirect) {
		Iterator<McpContent> contentIter = toolSpec.callStream(this, args);
		List<McpContent> contents = new ArrayList<>();
		McpCallToolResult result = new McpCallToolResult();
		Throwable iterError = null;
		try {
			if (returnDirect) {
				if (contentIter.hasNext()) {
					contents.add(contentIter.next());
				}
			} else {
				while (contentIter.hasNext()) {
					contents.add(contentIter.next());
				}
			}
		} catch (Exception e) {
			iterError = e;
			log.error("Streaming tool {} failed mid-stream", toolSpec.getTool().getName(), e);
		}
		if (iterError != null) {
			McpTextContent errorContent = new McpTextContent("Stream interrupted: " + iterError.getMessage());
			contents.add(errorContent);
			result.setError(Boolean.TRUE);
		}
		result.setContent(contents.isEmpty() ? Collections.emptyList() : contents);
		return result;
	}

	/**
	 * 判断是否关闭
	 * @return 是否关闭
	 */
	public boolean isClosed() {
		return httpStream == null || httpStream.isClosed();
	}

	/**
	 * 关闭底层流（如有）。
	 */
	public void close() {
		if (httpStream != null) {
			httpStream.close();
		}
	}

	@Override
	public String toString() {
		return "McpServerSession{" +
			"sessionId='" + sessionId + '\'' +
			", hasStream=" + hasStream() +
			'}';
	}
}
