package net.dreamlu.mica.net.http.mcp.server;

import net.dreamlu.mica.net.http.jsonrpc.JsonRpcError;

/**
 * MCP 服务端异常，转换为 JSON-RPC error response。
 *
 * <p>Handler 抛出该异常会被 {@link McpServer#handleIncomingRequest} 捕获，
 * 并转换为对应 code 的 JSON-RPC error 响应。</p>
 *
 * @author L.cm
 */
public class McpException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final int code;
	private final Object data;

	public McpException(int code, String message) {
		this(code, message, null, null);
	}

	public McpException(int code, String message, Object data) {
		this(code, message, data, null);
	}

	public McpException(int code, String message, Throwable cause) {
		this(code, message, null, cause);
	}

	public McpException(int code, String message, Object data, Throwable cause) {
		super(message, cause);
		this.code = code;
		this.data = data;
	}

	public int getCode() {
		return code;
	}

	public Object getData() {
		return data;
	}

	/**
	 * 转换为 JsonRpcError
	 *
	 * @return JsonRpcError
	 */
	public JsonRpcError toJsonRpcError() {
		JsonRpcError error = new JsonRpcError();
		error.setCode(code);
		error.setMessage(getMessage());
		if (data != null) {
			error.setData(data);
		}
		return error;
	}
}