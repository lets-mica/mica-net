package net.dreamlu.mica.net.http.mcp.server.transport;

import net.dreamlu.mica.net.http.common.HttpRequest;
import net.dreamlu.mica.net.http.common.HttpResponse;

/**
 * MCP 传输层接口。
 *
 * <p>每个实现负责把 MCP 协议绑定到具体的 HTTP/SSE 传输机制上。
 * 所有实现都应该支持会话超时清理（默认 30 分钟）和可选的心跳自动调度。</p>
 *
 * @author L.cm
 */
public interface McpTransport {

	/**
	 * 默认会话空闲超时时间（毫秒），30 分钟。
	 */
	long DEFAULT_SESSION_TIMEOUT_MS = 30L * 60L * 1000L;

	/**
	 * 默认 SSE 心跳间隔（毫秒），15 秒。
	 * &lt;=0 表示不自动发送心跳，需要业务侧手动调用 {@link #sendHeartbeat()}。
	 */
	long DEFAULT_HEARTBEAT_INTERVAL_MS = 15_000L;

	/**
	 * 处理 MCP 请求。
	 *
	 * @param request HttpRequest
	 * @return HttpResponse
	 */
	HttpResponse handle(HttpRequest request);

	/**
	 * 获取传输类型名称。
	 *
	 * @return 传输类型
	 */
	String getType();

	/**
	 * 向所有 session 发送心跳（手动触发）。
	 */
	void sendHeartbeat();

	/**
	 * 配置会话空闲超时时间。超过该时间没有访问的 session 将被自动清理。
	 *
	 * @param timeoutMs 超时毫秒数，&lt;=0 表示不清理
	 * @return this
	 */
	McpTransport sessionTimeout(long timeoutMs);

	/**
	 * 获取会话空闲超时时间（毫秒）。
	 *
	 * @return 超时毫秒数
	 */
	long getSessionTimeout();

	/**
	 * 配置 SSE 心跳间隔。
	 *
	 * @param intervalMs 心跳间隔毫秒数，&lt;=0 表示关闭自动心跳
	 * @return this
	 */
	McpTransport heartbeatInterval(long intervalMs);

	/**
	 * 获取 SSE 心跳间隔。
	 *
	 * @return 心跳间隔毫秒数
	 */
	long getHeartbeatInterval();

	/**
	 * 关闭 transport 及其持有的所有资源（线程、session 等）。
	 */
	void close();

}