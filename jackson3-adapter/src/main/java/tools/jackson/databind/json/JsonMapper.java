package tools.jackson.databind.json;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.cfg.MapperBuilder;

public class JsonMapper {

	public JsonMapper() {
	}

	public static Builder builder(JsonFactory factory) {
		return null;
	}

	public Builder rebuild() {
		return null;
	}

	public String writeValueAsString(Object object) {
		return null;
	}

	public byte[] writeValueAsBytes(Object object) {
		return null;
	}

	public <T> T readValue(Object json, Object object) {
		return null;
	}

	public JavaType getTypeFactory() {
		return null;
	}

	public <T> T convertValue(Object fromValue, Object toValueType) {
		return null;
	}

	public static class Builder extends MapperBuilder<JsonMapper, Builder> {

		public JsonMapper build() {
			return null;
		}
	}
}
