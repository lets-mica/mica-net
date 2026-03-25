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

package org.tio.http.common.stream;

import org.tio.core.Tio;
import org.tio.core.intf.Packet;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.sse.SseData;
import org.tio.utils.SysConst;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * HTTP Streaming 输出流，统一支持 Chunked Transfer-Encoding 和 SSE
 *
 * @author L.cm
 */
public class HttpStream extends OutputStream {
	private final HttpRequest request;
	private final HttpStreamType type;
	private volatile boolean closed = false;

	public HttpStream(HttpRequest request) {
		this(request, HttpStreamType.CHUNKED);
	}

	public HttpStream(HttpRequest request, HttpStreamType type) {
		this.request = request;
		this.type = type;
	}

	/**
	 * 发送原始数据
	 *
	 * @param data 数据
	 */
	public void send(byte[] data) {
		try {
			write(data);
		} catch (IOException e) {
			throw new RuntimeException("Failed to send data", e);
		}
	}

	/**
	 * 发送 SSE 数据
	 *
	 * @param sseData SseData
	 */
	public void send(SseData sseData) {
		sendString(sseData.toString());
	}

	/**
	 * 发送 SSE 事件（等同于 data 字段）
	 *
	 * @param data data
	 */
	public void send(Object data) {
		send(SseData.of(data));
	}

	/**
	 * 发送 SSE 事件
	 *
	 * @param event event
	 * @param data  data
	 */
	public void send(String event, Object data) {
		send(SseData.of(event, data));
	}

	/**
	 * 发送 SSE 事件
	 *
	 * @param id    id
	 * @param event event
	 * @param data  data
	 */
	public void send(String id, String event, Object data) {
		send(SseData.of(id, event, data));
	}

	/**
	 * 发送 SSE 事件
	 *
	 * @param id    id
	 * @param event event
	 * @param data  data
	 */
	public void send(long id, String event, Object data) {
		send(SseData.of(id, event, data));
	}

	/**
	 * 直接发送字符串
	 */
	private void sendString(String str) {
		try {
			write(str.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new RuntimeException("Failed to send data", e);
		}
	}

	@Override
	public void write(int b) throws IOException {
		write(new byte[]{(byte) b}, 0, 1);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (closed) {
			throw new IOException("Stream type: " + type + " already closed");
		}
		if (len <= 0) {
			return;
		}
		Packet packet = encodeChunk(b, off, len);
		Tio.send(request.channelContext, packet);
	}

	@Override
	public void flush() throws IOException {
		// HTTP chunked transfer doesn't require explicit flush
		// Data is sent immediately via Tio.send()
	}

	@Override
	public void close() throws IOException {
		if (closed) {
			return;
		}
		closed = true;
		if (type == HttpStreamType.CHUNKED) {
			// 发送最后一块，使用阻塞发送确保数据写入网络
			Tio.bSend(request.channelContext, encodeLastChunk());
		}
		// SSE 不发送终止块，但关闭连接
		request.close("Stream type: " + type + " closed");
	}

	/**
	 * 主动关闭流
	 */
	public void end() {
		try {
			close();
		} catch (IOException e) {
			// ignore
		}
	}

	/**
	 * Encode a chunk
	 */
	private Packet encodeChunk(byte[] data, int off, int len) {
		byte[] crlf = SysConst.CR_LF;
		ByteBuffer buffer;
		if (type == HttpStreamType.SSE) {
			// SSE: data already ends with \r\n from SseData.toString()
			buffer = ByteBuffer.allocate(len);
			buffer.put(data, off, len);
		} else {
			// CHUNKED: size\r\ndata\r\n
			String sizeHex = Integer.toHexString(len);
			byte[] sizeBytes = sizeHex.getBytes();
			buffer = ByteBuffer.allocate(sizeBytes.length + crlf.length + len + crlf.length);
			buffer.put(sizeBytes);
			buffer.put(crlf);
			buffer.put(data, off, len);
			buffer.put(crlf);
		}
		buffer.flip();
		Packet packet = new Packet();
		packet.setPreEncodedByteBuffer(buffer);
		return packet;
	}

	/**
	 * Encode the last chunk for CHUNKED transfer
	 */
	private Packet encodeLastChunk() {
		Packet packet = new Packet();
		ByteBuffer buffer = ByteBuffer.wrap("0\r\n\r\n".getBytes());
		packet.setPreEncodedByteBuffer(buffer);
		return packet;
	}

	/**
	 * Check if stream is closed
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * 获取流类型
	 */
	public HttpStreamType getType() {
		return type;
	}
}
