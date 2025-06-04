package org.tio.http.common.mcp.schema;

/**
 * Binary contents of a resource.
 *
 * @author L.cm
 */
public class McpBlobResourceContents implements McpResourceContents {
	/**
	 * the URI of this resource.
	 */
	private String uri;
	/**
	 * the MIME type of this resource.
	 */
	private String mimeType;
	/**
	 * a base64-encoded string representing the binary data of the resource.
	 * This must only be set if the resource can actually be represented as binary data
	 * (not text).
	 */
	private String blob;

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

	public String getBlob() {
		return blob;
	}

	public void setBlob(String blob) {
		this.blob = blob;
	}

	@Override
	public String toString() {
		return "McpBlobResourceContents{" +
			"uri='" + uri + '\'' +
			", mimeType='" + mimeType + '\'' +
			", blob='" + blob + '\'' +
			'}';
	}
}
