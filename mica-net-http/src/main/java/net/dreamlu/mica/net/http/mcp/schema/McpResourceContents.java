package net.dreamlu.mica.net.http.mcp.schema;

/**
 * The contents of a specific resource or sub-resource.
 */
public interface McpResourceContents {

	/**
	 * The URI of this resource.
	 *
	 * @return the URI of this resource.
	 */
	String getUri();

	/**
	 * The MIME type of this resource.
	 *
	 * @return the MIME type of this resource.
	 */
	String getMimeType();

}
