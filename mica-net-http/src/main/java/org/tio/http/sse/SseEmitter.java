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

package org.tio.http.sse;

import org.tio.core.Tio;
import org.tio.core.intf.Packet;
import org.tio.http.common.HeaderName;
import org.tio.http.common.HeaderValue;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;

import java.nio.ByteBuffer;

/**
 * sse 发射器
 *
 * @author L.cm
 */
public class SseEmitter {
	private final HttpRequest request;

	SseEmitter(HttpRequest request) {
		this.request = request;
	}

	/**
	 * 发送 sse 事件
	 *
	 * @param data data
	 */
	public void send(Object data) {
		this.send(new SseEvent().data(data));
	}

	/**
	 * 发送 sse 事件
	 *
	 * @param event event
	 * @param data  data
	 */
	public void send(String event, Object data) {
		this.send(new SseEvent().name(event).data(data));
	}

	/**
	 * 发送 sse 事件
	 *
	 * @param id    id
	 * @param event event
	 * @param data  data
	 */
	public void send(String id, String event, Object data) {
		this.send(new SseEvent().id(id).name(event).data(data));
	}

	/**
	 * 发送 sse 事件
	 *
	 * @param id    id
	 * @param event event
	 * @param data  data
	 */
	public void send(long id, String event, Object data) {
		this.send(new SseEvent().id(id).name(event).data(data));
	}

	/**
	 * 发送 sse 事件
	 *
	 * @param sseEvent SseEvent
	 */
	public void send(SseEvent sseEvent) {
		// 编码 sse 事件数据
		String sseEventData = sseEvent.toString();
		byte[] sseEventDataBytes = sseEventData.getBytes(request.getCharset());
		// 创建 sse packet 并设置预编码的 byte buffer
		Packet ssePacket = new Packet();
		ssePacket.setPreEncodedByteBuffer(ByteBuffer.wrap(sseEventDataBytes));
		// 发送 sse packet
		Tio.send(request.channelContext, ssePacket);
	}

	/**
	 * 主动关闭
	 */
	public void close() {
		request.close("主动关闭 sse 连接");
	}

	/**
	 * 获取 Chunked SseEmitter
	 *
	 * @param request  HttpRequest
	 * @param response HttpResponse
	 * @return SseChunkedEmitter
	 */
	public static SseEmitter getEmitter(HttpRequest request, HttpResponse response) {
		response.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.TEXT_EVENT_STREAM);
		response.addHeader(HeaderName.Cache_Control, HeaderValue.Cache_Control.no_cache);
		response.addHeader(HeaderName.Connection, HeaderValue.Connection.keep_alive);
		return new SseEmitter(request);
	}

}
