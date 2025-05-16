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
	/**
	 * 标记
	 */
	private static final byte[] ID_BYTES = { 105, 100, 58 };
	private static final byte[] EVENT_BYTES = { 101, 118, 101, 110, 116, 58 };
	private static final byte[] DATA_BYTES = { 100, 97, 116, 97, 58 };

	private final Charset charset;
	/**
	 * 事件标识（用于断线续传），是否必须：否
	 */
	private final String eventId;
	/**
	 * 自定义事件类型（如 progress），是否必须：否
	 */
	private final String event;
	/**
	 * 事件的实际内容，是否必须：是（除非只发 id）
	 */
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
		// 计算报文长度
		int size = 0;
		// eventId 长度
		byte[] eventIdBytes = null;
		if (eventId != null) {
			eventIdBytes = eventId.getBytes(charset);
			size += 3 + eventIdBytes.length + 2;
		}
		// event 长度
		byte[] eventBytes = null;
		if (event != null) {
			eventBytes = event.getBytes(charset);
			size += 6 + eventBytes.length + 2;
		}
		// data 长度
		if (data != null) {
			size += 5 + 2 + data.length;
		}
		// 换行长度
		size += SysConst.CR_LF.length;
		// 构造 ByteBuffer
		ByteBuffer buffer = ByteBuffer.allocate(size);
		// 浏览器运行环境的字节序（通常为小端序）
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		// 添加 id
		if (eventIdBytes != null) {
			buffer.put(ID_BYTES);
			buffer.put(eventIdBytes);
			buffer.put(SysConst.CR_LF);
		}
		// 添加 event
		if (eventBytes != null) {
			buffer.put(EVENT_BYTES);
			buffer.put(eventBytes);
			buffer.put(SysConst.CR_LF);
		}
		// 添加 data 数据
		if (data != null) {
			buffer.put(DATA_BYTES);
			buffer.put(data);
			buffer.put(SysConst.CR_LF);
		}
		buffer.put(SysConst.CR_LF);
		return buffer;
	}

	public static class Builder {
		private Charset charset = StandardCharsets.UTF_8;
		private String eventId;
		private String event;
		private byte[] data;

		public Builder charset(Charset charset) {
			this.charset = charset;
			return this;
		}

		public Builder eventId(String eventId) {
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
