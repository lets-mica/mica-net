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
import org.tio.core.intf.EncodedPacket;
import org.tio.http.common.HeaderName;
import org.tio.http.common.HeaderValue;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.utils.SysConst;

import java.nio.charset.StandardCharsets;

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
		String chunkedString = sseEvent.toString();
		byte[] chunkedBytes = chunkedString.getBytes(request.getCharset());
		EncodedPacket encodedPacket = new EncodedPacket(encodeChunk(chunkedBytes));
		Tio.send(request.channelContext, encodedPacket);
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
		response.addHeader(HeaderName.Connection, HeaderValue.Connection.keep_alive);
		response.addHeader(HeaderName.Transfer_Encoding, HeaderValue.Transfer_Encoding.chunked);
		return new SseEmitter(request);
	}

	/**
	 * 编码 chunk 数据
	 *
	 * @param chunkData chunk 数据
	 * @return 编码后的 chunk 数据
	 */
	private static byte[] encodeChunk(byte[] chunkData) {
		int length = chunkData.length;
		String chunkSize = Integer.toHexString(length);
		byte[] chunkSizeBytes = (chunkSize + SysConst.CRLF).getBytes(StandardCharsets.US_ASCII);

		byte[] chunk = new byte[chunkSizeBytes.length + length + 2];

		System.arraycopy(chunkSizeBytes, 0, chunk, 0, chunkSizeBytes.length);
		System.arraycopy(chunkData, 0, chunk, chunkSizeBytes.length, length);
		System.arraycopy(SysConst.CR_LF, 0, chunk, chunkSizeBytes.length + length, 2);
		return chunk;
	}

}
