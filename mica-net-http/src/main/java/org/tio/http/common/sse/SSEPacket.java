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

import org.tio.core.intf.Packet;
import org.tio.utils.SysConst;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * SSEPacket
 *
 * @author L.cm
 */
public final class SSEPacket extends Packet {
	private static final byte[] idBytes = { 105, 100, 58 };
	private static final byte[] eventBytes = { 101, 118, 101, 110, 116, 58 };
	private static final byte[] dataBytes = { 100, 97, 116, 97, 58 };

	private final Charset charset;
	private final Long eventId;
	private final String event;
	private final byte[] data;

	private SSEPacket(Builder builder) {
		super();
		this.charset = builder.charset;
		this.eventId = builder.eventId;
		this.event = builder.event;
		this.data = builder.data;
	}

	@Override
	public ByteBuffer getPreEncodedByteBuffer() {
		ByteBuffer buffer = ByteBuffer.allocate(calculateBufferSize());
		// 浏览器运行环境的字节序（通常为小端序）
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		// Add id
		if (eventId != null) {
			buffer.put(idBytes);
			buffer.put(eventId.toString().getBytes(charset)).put(SysConst.CR_LF);
		}
		// Add event
		if (event != null) {
			buffer.put(eventBytes).put(event.getBytes(charset)).put(SysConst.CR_LF);
		}
		// Add data
		if (data != null) {
			buffer.put(dataBytes);
			buffer.put(data);
			buffer.put(SysConst.CR_LF);

		}
		buffer.put(SysConst.CR_LF);
		buffer.flip();
		return buffer;
	}

	private int calculateBufferSize() {
		int size = 0;
		// id
		if (eventId != null) {
			size += 3 + Long.toString(eventId).getBytes(charset).length + 2;
		}
		// event
		if (event != null) {
			size += 6 + event.getBytes(charset).length + 2;
		}
		// data
		if (data != null) {
			size += 5 + 2 + data.length;
		}
		// Add extra newline
		size += SysConst.CR_LF.length;
		return size;
	}

	public static class Builder {
		private Charset charset = StandardCharsets.UTF_8;
		private Long eventId;
		private String event;
		private byte[] data;

		public Builder charset(Charset charset) {
			this.charset = charset;
			return this;
		}

		public Builder eventId(Long eventId) {
			this.eventId = eventId;
			return this;
		}

		public Builder event(String event) {
			this.event = event;
			return this;
		}

		public Builder data(byte[] data) {
			this.data = data;
			return this;
		}

		public SSEPacket build() {
			return new SSEPacket(this);
		}
	}
}
