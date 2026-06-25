package net.dreamlu.mica.net.http.mcp.schema;

/**
 * Parameters for the {@code sampling/createMessage} request from the server.
 *
 * <p>The server uses this to ask the client LLM to generate a completion.</p>
 *
 * @author L.cm
 */
public class McpSampling {
	/**
	 * The user's request or question.
	 */
	private String prompt;
	/**
	 * Optional model hint.
	 */
	private String modelHint;
	/**
	 * Maximum tokens to generate.
	 */
	private Integer maxTokens;
	/**
	 * Sampling temperature.
	 */
	private Double temperature;

	public String getPrompt() {
		return prompt;
	}

	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	public String getModelHint() {
		return modelHint;
	}

	public void setModelHint(String modelHint) {
		this.modelHint = modelHint;
	}

	public Integer getMaxTokens() {
		return maxTokens;
	}

	public void setMaxTokens(Integer maxTokens) {
		this.maxTokens = maxTokens;
	}

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	@Override
	public String toString() {
		return "McpSampling{" +
			"prompt='" + prompt + '\'' +
			", modelHint='" + modelHint + '\'' +
			", maxTokens=" + maxTokens +
			", temperature=" + temperature +
			'}';
	}
}