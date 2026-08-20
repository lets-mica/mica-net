package net.dreamlu.mica.net.http.mcp.schema;

import java.util.Arrays;
import java.util.List;

/**
 * mcp 定义
 *
 * <p>
 * <a href="https://modelcontextprotocol.io/specification/2025-06-18">mcp 协议地址</a>
 * </p>
 *
 * @author L.cm
 */
public interface McpSchema {

	/**
	 * MCP protocol version for 2024-11-05.
	 * https://modelcontextprotocol.io/specification/2024-11-05
	 */
	String MCP_2024_11_05 = "2024-11-05";

	/**
	 * MCP protocol version for 2025-03-26.
	 * https://modelcontextprotocol.io/specification/2025-03-26
	 */
	String MCP_2025_03_26 = "2025-03-26";

	/**
	 * MCP protocol version for 2025-06-18.
	 * https://modelcontextprotocol.io/specification/2025-06-18
	 */
	String MCP_2025_06_18 = "2025-06-18";

	/**
	 * MCP protocol version for 2025-11-25.
	 * https://modelcontextprotocol.io/specification/2025-11-25
	 */
	String MCP_2025_11_25 = "2025-11-25";

	/**
	 * MCP protocol version for 2026-07-28.
	 * <p>无状态协议核心：删除 initialize / Mcp-Session-Id，新增 server/discover 与 Multi Round-Trip Requests。</p>
	 * https://modelcontextprotocol.io/specification/2026-07-28
	 */
	String MCP_2026_07_28 = "2026-07-28";

	/**
	 * 当前 mica-net 默认实现的协议版本（modern）。
	 */
	String MCP_LATEST = MCP_2026_07_28;

	/**
	 * mcp 版本列表
	 */
	List<String> MCP_VERSION_LIST = Arrays.asList(
		MCP_2024_11_05, MCP_2025_03_26, MCP_2025_06_18, MCP_2025_11_25, MCP_2026_07_28);

	String JSONRPC_VERSION = "2.0";

	// ---------------------------
	// Method Names
	// ---------------------------

	// Lifecycle Methods (legacy: 2025-11-25 及更早)
	String METHOD_INITIALIZE = "initialize";

	String METHOD_NOTIFICATION_INITIALIZED = "notifications/initialized";

	String METHOD_PING = "ping";

	// Lifecycle Methods (modern: 2026-07-28)
	/**
	 * server/discover：modern 协议必实现的 RPC，用于声明 server 的协议版本、capabilities、identity。
	 */
	String METHOD_SERVER_DISCOVER = "server/discover";

	/**
	 * server 信息在 _meta 中的字段名。
	 */
	String META_SERVER_INFO = "io.modelcontextprotocol/serverInfo";

	/**
	 * client 信息在 _meta 中的字段名。
	 */
	String META_CLIENT_INFO = "io.modelcontextprotocol/clientInfo";

	/**
	 * 客户端能力声明在 _meta 中的字段名。
	 */
	String META_CLIENT_CAPABILITIES = "io.modelcontextprotocol/clientCapabilities";

	/**
	 * 协议版本在 _meta 中的字段名。
	 */
	String META_PROTOCOL_VERSION = "io.modelcontextprotocol/protocolVersion";

	/**
	 * Result 类型：普通响应。
	 */
	String RESULT_TYPE_COMPLETE = "complete";

	/**
	 * Result 类型：MRTR 中间响应（需要 client 提供更多信息）。
	 */
	String RESULT_TYPE_INPUT_REQUIRED = "input_required";

	/**
	 * Streamable HTTP POST 必需 Header：方法名。
	 */
	String HEADER_MCP_METHOD = "Mcp-Method";

	/**
	 * Streamable HTTP POST 必需 Header：tool/prompt/resource 名。
	 */
	String HEADER_MCP_NAME = "Mcp-Name";

	// Tool Methods
	String METHOD_TOOLS_LIST = "tools/list";

	String METHOD_TOOLS_CALL = "tools/call";

	String METHOD_NOTIFICATION_TOOLS_LIST_CHANGED = "notifications/tools/list_changed";

	// Resources Methods
	String METHOD_RESOURCES_LIST = "resources/list";

	String METHOD_RESOURCES_READ = "resources/read";

	String METHOD_NOTIFICATION_RESOURCES_UPDATED  = "notifications/resources/updated";

	String METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";

	String METHOD_RESOURCES_TEMPLATES_LIST = "resources/templates/list";

	String METHOD_RESOURCES_SUBSCRIBE = "resources/subscribe";

	String METHOD_RESOURCES_UNSUBSCRIBE = "resources/unsubscribe";

	// Prompt Methods
	String METHOD_PROMPT_LIST = "prompts/list";

	String METHOD_PROMPT_GET = "prompts/get";

	String METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";

	// Logging Methods
	String METHOD_LOGGING_SET_LEVEL = "logging/setLevel";

	String METHOD_NOTIFICATION_MESSAGE = "notifications/message";

	// Roots Methods
	String METHOD_ROOTS_LIST = "roots/list";

	String METHOD_NOTIFICATION_ROOTS_LIST_CHANGED = "notifications/roots/list_changed";

	// Sampling Methods
	String METHOD_SAMPLING_CREATE_MESSAGE = "sampling/createMessage";

	// Progress / Cancellation
	String METHOD_NOTIFICATION_PROGRESS = "notifications/progress";

	String METHOD_NOTIFICATION_CANCELLED = "notifications/cancelled";

	// Completion Methods
	String METHOD_COMPLETION_COMPLETE = "completion/complete";

}
