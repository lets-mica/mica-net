package org.tio.http.mcp.schema;

/**
 * mcp 定义
 *
 * @author L.cm
 */
public interface McpSchema {

	String LATEST_PROTOCOL_VERSION = "2024-11-05";

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

}
