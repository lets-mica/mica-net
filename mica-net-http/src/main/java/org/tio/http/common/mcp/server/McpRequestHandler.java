package org.tio.http.common.mcp.server;

/**
 * A handler for client requests.
 *
 * @param <T> the type of the response that is expected as a result of handling the
 *            request.
 */
@FunctionalInterface
public interface McpRequestHandler<T> {

	/**
	 * Handles a request from the client.
	 *
	 * @param mcpServer the exchange associated with the client that allows calling
	 *                  back to the connected client or inspecting its capabilities.
	 * @param params    the parameters of the request.
	 * @return value that will emit the response to the request.
	 */
	T handle(McpServer mcpServer, Object params);

}
