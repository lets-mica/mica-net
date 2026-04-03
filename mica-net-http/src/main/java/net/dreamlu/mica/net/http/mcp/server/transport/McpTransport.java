package net.dreamlu.mica.net.http.mcp.server.transport;

import net.dreamlu.mica.net.http.common.HttpRequest;
import net.dreamlu.mica.net.http.common.HttpResponse;

/**
 * MCP 传输层接口
 *
 * @author L.cm
 */
public interface McpTransport {

	/**
	 * 处理 MCP 请求
	 *
	 * @param request HttpRequest
	 * @return HttpResponse
	 */
	HttpResponse handle(HttpRequest request);

	/**
	 * 获取传输类型名称
	 *
	 * @return 传输类型
	 */
	String getType();

	/**
	 * 向所有 session 发送心跳
	 */
	void sendHeartbeat();
}
