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
 * JSON-RPC Error Codes
 *
 * @author L.cm
 */
public interface JsonRpcErrorCodes {

	/**
	 * Invalid JSON was received by the server.
	 */
	int PARSE_ERROR = -32700;

	/**
	 * The JSON sent is not a valid Request object.
	 */
	int INVALID_REQUEST = -32600;

	/**
	 * The method does not exist / is not available.
	 */
	int METHOD_NOT_FOUND = -32601;

	/**
	 * Invalid method parameter(s).
	 */
	int INVALID_PARAMS = -32602;

	/**
	 * Internal JSON-RPC error.
	 */
	int INTERNAL_ERROR = -32603;

}
