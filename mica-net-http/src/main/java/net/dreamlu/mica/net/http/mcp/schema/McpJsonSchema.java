package net.dreamlu.mica.net.http.mcp.schema;

import java.util.List;
import java.util.Map;

/**
 * mcp JSON Schema（2020-12 兼容）。
 *
 * <p>2026-07-28 协议升级到完整 JSON Schema 2020-12 支持，
 * 新增组合关键字（allOf / anyOf / oneOf / not）、$ref / $defs / $schema、prefixItems、
 * const、enum、nullable、format、minimum / maximum / exclusiveMinimum 等约束字段。</p>
 *
 * <p>原字段（type / properties / required / additionalProperties）保留以兼容 legacy；
 * 新增字段以可选 getter/setter 暴露，业务可按需填充。</p>
 *
 * @author L.cm
 */
public class McpJsonSchema {
	// === 基础类型（Draft-04 兼容）===
	private String type;
	private Map<String, Object> properties;
	private List<String> required;
	private Boolean additionalProperties;

	// === Draft 2020-12 元信息 ===
	/**
	 * 完整的 schema URI，例如 {@code "https://json-schema.org/draft/2020-12/schema"}。
	 */
	private String schema;
	/**
	 * schema id / anchor，可用于跨文档引用。
	 */
	private String id;
	/**
	 * 锚点名称，配合 $ref 使用。
	 */
	private String anchor;
	/**
	 * 局部 reusable 子 schema 定义，配合 {@code $ref: "#/$defs/Foo"} 使用。
	 */
	private Map<String, Object> defs;

	// === Draft 2020-12 组合关键字 ===
	/**
	 * 数组元素对应的 tuple schema（替代 draft-04 的 items 数组语义）；
	 * 同时支持旧 {@code items}（单 schema 或数组）。
	 */
	private Object items;
	private List<Object> prefixItems;
	private Object additionalItems;
	private List<Object> allOf;
	private List<Object> anyOf;
	private List<Object> oneOf;
	private Object not;

	// === 约束字段 ===
	private Object constValue;
	private List<Object> enumValues;
	private Boolean nullable;
	private String format;
	private String pattern;
	private Object minimum;
	private Object maximum;
	private Object exclusiveMinimum;
	private Object exclusiveMaximum;
	private Object minLength;
	private Object maxLength;
	private Integer minItems;
	private Integer maxItems;
	private Boolean uniqueItems;
	private Object minProperties;
	private Object maxProperties;
	private String description;
	private String title;
	private Object defaultValue;
	private List<String> examples;

	// === 引用 ===
	/**
	 * JSON Pointer / JSON Reference，指向本 schema 或外部 schema。
	 */
	private String ref;

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

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAnchor() {
		return anchor;
	}

	public void setAnchor(String anchor) {
		this.anchor = anchor;
	}

	public Map<String, Object> getDefs() {
		return defs;
	}

	public void setDefs(Map<String, Object> defs) {
		this.defs = defs;
	}

	public Object getItems() {
		return items;
	}

	public void setItems(Object items) {
		this.items = items;
	}

	public List<Object> getPrefixItems() {
		return prefixItems;
	}

	public void setPrefixItems(List<Object> prefixItems) {
		this.prefixItems = prefixItems;
	}

	public Object getAdditionalItems() {
		return additionalItems;
	}

	public void setAdditionalItems(Object additionalItems) {
		this.additionalItems = additionalItems;
	}

	public List<Object> getAllOf() {
		return allOf;
	}

	public void setAllOf(List<Object> allOf) {
		this.allOf = allOf;
	}

	public List<Object> getAnyOf() {
		return anyOf;
	}

	public void setAnyOf(List<Object> anyOf) {
		this.anyOf = anyOf;
	}

	public List<Object> getOneOf() {
		return oneOf;
	}

	public void setOneOf(List<Object> oneOf) {
		this.oneOf = oneOf;
	}

	public Object getNot() {
		return not;
	}

	public void setNot(Object not) {
		this.not = not;
	}

	public Object getConstValue() {
		return constValue;
	}

	public void setConstValue(Object constValue) {
		this.constValue = constValue;
	}

	public List<Object> getEnumValues() {
		return enumValues;
	}

	public void setEnumValues(List<Object> enumValues) {
		this.enumValues = enumValues;
	}

	public Boolean getNullable() {
		return nullable;
	}

	public void setNullable(Boolean nullable) {
		this.nullable = nullable;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public Object getMinimum() {
		return minimum;
	}

	public void setMinimum(Object minimum) {
		this.minimum = minimum;
	}

	public Object getMaximum() {
		return maximum;
	}

	public void setMaximum(Object maximum) {
		this.maximum = maximum;
	}

	public Object getExclusiveMinimum() {
		return exclusiveMinimum;
	}

	public void setExclusiveMinimum(Object exclusiveMinimum) {
		this.exclusiveMinimum = exclusiveMinimum;
	}

	public Object getExclusiveMaximum() {
		return exclusiveMaximum;
	}

	public void setExclusiveMaximum(Object exclusiveMaximum) {
		this.exclusiveMaximum = exclusiveMaximum;
	}

	public Object getMinLength() {
		return minLength;
	}

	public void setMinLength(Object minLength) {
		this.minLength = minLength;
	}

	public Object getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(Object maxLength) {
		this.maxLength = maxLength;
	}

	public Integer getMinItems() {
		return minItems;
	}

	public void setMinItems(Integer minItems) {
		this.minItems = minItems;
	}

	public Integer getMaxItems() {
		return maxItems;
	}

	public void setMaxItems(Integer maxItems) {
		this.maxItems = maxItems;
	}

	public Boolean getUniqueItems() {
		return uniqueItems;
	}

	public void setUniqueItems(Boolean uniqueItems) {
		this.uniqueItems = uniqueItems;
	}

	public Object getMinProperties() {
		return minProperties;
	}

	public void setMinProperties(Object minProperties) {
		this.minProperties = minProperties;
	}

	public Object getMaxProperties() {
		return maxProperties;
	}

	public void setMaxProperties(Object maxProperties) {
		this.maxProperties = maxProperties;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	public List<String> getExamples() {
		return examples;
	}

	public void setExamples(List<String> examples) {
		this.examples = examples;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	@Override
	public String toString() {
		return "McpJsonSchema{" +
			"type='" + type + '\'' +
			", properties=" + properties +
			", required=" + required +
			", additionalProperties=" + additionalProperties +
			", ref='" + ref + '\'' +
			", schema='" + schema + '\'' +
			'}';
	}
}
