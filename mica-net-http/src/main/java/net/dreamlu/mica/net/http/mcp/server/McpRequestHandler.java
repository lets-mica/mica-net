package net.dreamlu.mica.net.http.mcp.server;

import net.dreamlu.mica.net.http.jsonrpc.JsonRpcRequest;
import net.dreamlu.mica.net.http.jsonrpc.JsonRpcResponse;

/**
 * mcp 请求 handler
 *
 * <p>每个 handler 负责处理一个 JSON-RPC method，并返回 JSON-RPC 响应。
 * 可以抛出 {@link McpException} 表达可恢复的业务错误，
 * 其他异常会被 {@link McpServer#handleIncomingRequest} 统一捕获并转换为 INTERNAL_ERROR。</p>
 *
 * @author L.cm
 */
@FunctionalInterface
public interface McpRequestHandler {

	/**
	 * 处理一个 JSON-RPC 请求。
	 *
	 * @param session 当前 session
	 * @param request JSON-RPC 请求
	 * @return JSON-RPC 响应（成功或失败）
	 */
	JsonRpcResponse handle(McpServerSession session, JsonRpcRequest request);

}