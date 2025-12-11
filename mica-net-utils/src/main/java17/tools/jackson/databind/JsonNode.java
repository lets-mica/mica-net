package tools.jackson.databind;

public abstract class JsonNode {

	public boolean isArray() {
		return true;
	}

	public boolean isObject() {
		return true;
	}

}
