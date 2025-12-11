package tools.jackson.databind.json;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.type.TypeFactory;

public class JsonMapper extends ObjectMapper {

	public JsonMapper() {
	}

	public static Builder builder(JsonFactory factory) {
		return new Builder();
	}

	public Builder rebuild() {
		return new Builder();
	}

	public String writeValueAsString(Object object) {
		return null;
	}

	public byte[] writeValueAsBytes(Object object) {
		return null;
	}

	public <T> T readValue(String content, Class<T> valueType) {
		return null;
	}

	public <T> T readValue(String content, JavaType valueType) {
		return null;
	}

	public <T> T readValue(byte[] content, Class<T> valueType) {
		return null;
	}

	public <T> T readValue(byte[] content, JavaType valueType) {
		return null;
	}

	public TypeFactory getTypeFactory() {
		return null;
	}

	public <T> T convertValue(Object fromValue, Class<T> toValueType) {
		return null;
	}

	public <T> T convertValue(Object fromValue, JavaType toValueType) {
		return null;
	}

	public static class Builder extends MapperBuilder<JsonMapper, Builder> {

		public JsonMapper build() {
			return new JsonMapper();
		}
	}
}
