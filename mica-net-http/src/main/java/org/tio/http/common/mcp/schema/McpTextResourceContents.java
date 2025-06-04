package org.tio.http.common.mcp.schema;

/**
 * Text contents of a resource.
 *
 * @author L.cm
 */
public class McpTextResourceContents implements McpResourceContents {
	/**
	 * the URI of this resource.
	 */
	private String uri;
	/**
	 * the MIME type of this resource.
	 */
	private String mimeType;
	/**
	 * the text of the resource. This must only be set if the resource can
	 * actually be represented as text (not binary data).
	 */
	private String text;

	@Override
	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	@Override
	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return "McpTextResourceContents{" +
			"uri='" + uri + '\'' +
			", mimeType='" + mimeType + '\'' +
			", text='" + text + '\'' +
			'}';
	}
}
