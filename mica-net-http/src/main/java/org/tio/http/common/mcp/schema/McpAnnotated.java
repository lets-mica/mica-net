package org.tio.http.common.mcp.schema;

/**
 * Resource Interfaces Base for objects that include optional annotations for the client. The client can
 * use annotations to inform how objects are used or displayed
 *
 * @author L.cm
 */
public interface McpAnnotated {

	McpAnnotations getAnnotations();

}
