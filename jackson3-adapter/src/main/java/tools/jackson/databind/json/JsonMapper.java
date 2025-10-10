package tools.jackson.databind.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationFeature;

import java.util.function.UnaryOperator;

public class JsonMapper {

	public JsonMapper() {
	}

	public static Builder builder(JsonFactory factory) {
		return new Builder();
	}

	public JsonFactory rebuild() {
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

	public static class Builder {

		public Builder findAndAddModules() {
			return this;
		}

		public Builder disable(SerializationFeature serializationFeature) {
			return this;
		}

		public Builder changeDefaultPropertyInclusion(UnaryOperator<JsonInclude.Value> handler) {
			return this;
		}

		public Builder configure(DeserializationFeature deserializationFeature, boolean b) {
			return this;
		}

		public JsonMapper build() {
			return null;
		}
	}
}
