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

package org.tio.http.common.sse;

import org.tio.core.ChannelContext;
import org.tio.http.common.HeaderName;
import org.tio.http.common.HeaderValue;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;

/**
 * sse 发射器
 *
 * @author L.cm
 */
public interface SseEmitter<T> {

	/**
	 * 发送 sse 包
	 *
	 * @param packet packet
	 */
	void push(T packet);

	/**
	 * 主动关闭
	 */
	void close();

	/**
	 * 获取  SseEmitter
	 *
	 * @param request  HttpRequest
	 * @param response HttpResponse
	 * @return SseDefaultEmitter
	 */
	static SseDefaultEmitter getSseEmitter(HttpRequest request, HttpResponse response) {
		response.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.TEXT_EVENT_STREAM);
		response.addHeader(HeaderName.Connection, HeaderValue.Connection.keep_alive);
		ChannelContext context = request.getChannelContext();
		return new SseDefaultEmitter(context);
	}

	/**
	 * 获取 Chunked SseEmitter
	 *
	 * @param request  HttpRequest
	 * @param response HttpResponse
	 * @return SseChunkedEmitter
	 */
	static SseChunkedEmitter getSseChunkedEmitter(HttpRequest request, HttpResponse response) {
		response.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.TEXT_EVENT_STREAM);
		response.addHeader(HeaderName.Connection, HeaderValue.Connection.keep_alive);
		response.addHeader(HeaderName.Transfer_Encoding, HeaderValue.Transfer_Encoding.chunked);
		ChannelContext context = request.getChannelContext();
		return new SseChunkedEmitter(context);
	}

	/**
	 * 新的 sse 构造器
	 *
	 * @return SsePacket Builder
	 */
	static SsePacket.Builder sseBuilder() {
		return new SsePacket.Builder();
	}

	/**
	 * 新的 sse chunked 构造器
	 *
	 * @return SseChunkedPacket Builder
	 */
	static SseChunkedPacket.Builder sseChunkedBuilder() {
		return new SseChunkedPacket.Builder();
	}

}
