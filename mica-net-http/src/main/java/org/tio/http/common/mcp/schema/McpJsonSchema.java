package org.tio.http.common.mcp.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * mcp json Schema
 *
 * @author L.cm
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class McpJsonSchema {
	private String type;
	private Map<String, Object> properties;
	private List<String> required;
	private Boolean additionalProperties;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	public List<String> getRequired() {
		return required;
	}

	public void setRequired(List<String> required) {
		this.required = required;
	}

	public Boolean getAdditionalProperties() {
		return additionalProperties;
	}

	public void setAdditionalProperties(Boolean additionalProperties) {
		this.additionalProperties = additionalProperties;
	}

	@Override
	public String toString() {
		return "McpJsonSchema{" +
			"type='" + type + '\'' +
			", properties=" + properties +
			", required=" + required +
			", additionalProperties=" + additionalProperties +
			'}';
	}
}
