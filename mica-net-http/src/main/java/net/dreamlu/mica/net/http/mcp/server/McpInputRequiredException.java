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

package net.dreamlu.mica.net.http.mcp.server;

import net.dreamlu.mica.net.http.jsonrpc.JsonRpcErrorCodes;
import net.dreamlu.mica.net.http.mcp.schema.McpInputRequiredResult;

/**
 * MRTR 中间响应异常。
 *
 * <p>当 tool handler 在执行过程中需要 client 补充输入时，抛出本异常；
 * transport 会将其转换为 modern 协议下的 {@code resultType: "input_required"} 中间响应，
 * 或 legacy 协议下的错误响应。</p>
 *
 * @author L.cm
 */
public class McpInputRequiredException extends McpException {

	/**
	 * 中间响应数据。
	 */
	private final McpInputRequiredResult result;

	/**
	 * 构造 input_required 中间响应异常。
	 *
	 * @param result 中间响应数据
	 */
	public McpInputRequiredException(McpInputRequiredResult result) {
		super(JsonRpcErrorCodes.INTERNAL_ERROR, "Input required: " + (result == null ? null : result.getPrompt()));
		this.result = result;
	}

	public McpInputRequiredResult getResult() {
		return result;
	}
}