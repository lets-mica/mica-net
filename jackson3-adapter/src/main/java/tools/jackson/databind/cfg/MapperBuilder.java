package tools.jackson.databind.cfg;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.UnaryOperator;

public abstract class MapperBuilder<M extends JsonMapper, B extends MapperBuilder<M, B>> {

	public B findAndAddModules() {
		return null;
	}

	public B disable(SerializationFeature... serializationFeature) {
		return null;
	}

	public B changeDefaultPropertyInclusion(UnaryOperator<JsonInclude.Value> handler) {
		return null;
	}

	public B configure(DeserializationFeature deserializationFeature, boolean b) {
		return null;
	}

}
