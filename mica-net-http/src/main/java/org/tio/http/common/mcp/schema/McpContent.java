package org.tio.http.common.mcp.schema;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * mcp 内容
 *
 * @author L.cm
 */
@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	include = JsonTypeInfo.As.PROPERTY,
	property = "type"
)
@JsonSubTypes({
	@JsonSubTypes.Type(value = McpTextContent.class, name = "text"),
	@JsonSubTypes.Type(value = McpImageContent.class, name = "image"),
	@JsonSubTypes.Type(value = McpEmbeddedResource.class, name = "resource")
})
public interface McpContent {

}
