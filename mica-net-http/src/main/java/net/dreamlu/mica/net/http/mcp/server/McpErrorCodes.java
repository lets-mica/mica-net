package net.dreamlu.mica.net.http.mcp.server;

/**
 * MCP 错误码。
 *
 * <p>标准 JSON-RPC 错误码请参考
 * {@link net.dreamlu.mica.net.http.jsonrpc.JsonRpcErrorCodes}，
 * 本接口仅定义 MCP 特有的服务器错误码（范围 -32000 ~ -32099）。</p>
 *
 * @author L.cm
 */
public interface McpErrorCodes {

	/**
	 * 请求的资源不存在
	 */
	int RESOURCE_NOT_FOUND = -32002;

	/**
	 * 请求的工具不存在
	 */
	int TOOL_NOT_FOUND = -32004;

	/**
	 * 请求的 prompt 不存在
	 */
	int PROMPT_NOT_FOUND = -32005;

}