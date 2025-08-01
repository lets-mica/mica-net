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

package org.tio.http.jsonrpc;

/**
 * json rpc 响应
 *
 * @author L.cm
 */
public class JsonRpcResponse implements JsonRpcMessage {
	private String jsonrpc;
	private Object id;
	private Object result;
	private JsonRpcError error;

	@Override
	public String getJsonrpc() {
		return jsonrpc;
	}

	public void setJsonrpc(String jsonrpc) {
		this.jsonrpc = jsonrpc;
	}

	public Object getId() {
		return id;
	}

	public void setId(Object id) {
		this.id = id;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public JsonRpcError getError() {
		return error;
	}

	public void setError(JsonRpcError error) {
		this.error = error;
	}

	@Override
	public String toString() {
		return "JsonRpcResponse{" +
			"jsonrpc='" + jsonrpc + '\'' +
			", id=" + id +
			", result=" + result +
			", error=" + error +
			'}';
	}
}
