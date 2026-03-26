package org.tio.http.mcp.server.transport;

import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;

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
}
