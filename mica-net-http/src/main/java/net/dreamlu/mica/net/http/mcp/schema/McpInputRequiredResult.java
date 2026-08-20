/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & dreamlu.net).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dreamlu.mica.net.http.mcp.schema;

import java.util.List;
import java.util.Map;

/**
 * MRTR（Multi Round-R Trip Requests）中间响应。
 *
 * <p>2026-07-28+ modern 协议下，server→client 请求（如 sampling/createMessage、elicitation）改为多轮交互：
 * 当 server 在处理 tools/call 等请求时需要进一步询问 client，
 * 可返回 {@code resultType: "input_required"} 的中间结果，
 * client 收集用户/模型输入后携带 {@code _meta.mrtr.continuation} 字段再次发起原请求。</p>
 *
 * <p>注意：本类只在 modern 协议下生效；legacy 协议下应使用 {@link McpCallToolResult}。</p>
 *
 * @author L.cm
 */
public class McpInputRequiredResult {
	/**
	 * 询问 client 的提示文本（人类可读）。
	 */
	private String prompt;
	/**
	 * 结构化询问内容（可选）。例如询问用户选择、表单字段等。
	 */
	private List<Map<String, Object>> questions;
	/**
	 * 期望的输入 schema（JSON Schema 2020-12）。
	 * <p>client 在补齐输入时应按此 schema 校验。</p>
	 */
	private McpJsonSchema inputSchema;
	/**
	 * 本次中间响应的标识（可由 server 生成、返回原 id 关联）。
	 */
	private String continuationId;

	public String getPrompt() {
		return prompt;
	}

	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	public List<Map<String, Object>> getQuestions() {
		return questions;
	}

	public void setQuestions(List<Map<String, Object>> questions) {
		this.questions = questions;
	}

	public McpJsonSchema getInputSchema() {
		return inputSchema;
	}

	public void setInputSchema(McpJsonSchema inputSchema) {
		this.inputSchema = inputSchema;
	}

	public String getContinuationId() {
		return continuationId;
	}

	public void setContinuationId(String continuationId) {
		this.continuationId = continuationId;
	}

	@Override
	public String toString() {
		return "McpInputRequiredResult{" +
			"prompt='" + prompt + '\'' +
			", continuationId='" + continuationId + '\'' +
			", inputSchema=" + inputSchema +
			'}';
	}
}