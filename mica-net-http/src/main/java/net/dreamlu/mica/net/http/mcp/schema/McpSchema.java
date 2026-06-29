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
	 * mcp 版本列表
	 */
	List<String> MCP_VERSION_LIST = Arrays.asList(MCP_2024_11_05, MCP_2025_03_26, MCP_2025_06_18, MCP_2025_11_25);

	String JSONRPC_VERSION = "2.0";

	// ---------------------------
	// Method Names
	// ---------------------------

	// Lifecycle Methods
	String METHOD_INITIALIZE = "initialize";

	String METHOD_NOTIFICATION_INITIALIZED = "notifications/initialized";

	String METHOD_PING = "ping";

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
