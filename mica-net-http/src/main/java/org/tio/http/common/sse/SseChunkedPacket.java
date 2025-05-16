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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * sse 分片报文
 *
 * @author L.cm
 */
public class SseChunkedPacket {

	/**
	 * 字符集
	 */
	private final Charset charset;
	/**
	 * 事件标识（用于断线续传），是否必须：否
	 */
	private final String id;
	/**
	 * 自定义事件类型（如 progress），是否必须：否
	 */
	private final String event;
	/**
	 * 事件的实际内容，是否必须：是（除非只发 id）
	 */
	private final String data;

	private SseChunkedPacket(Builder builder) {
		this.charset = builder.charset;
		this.id = builder.id;
		this.event = builder.event;
		this.data = builder.data;
	}

	public byte[] getChunkedBytes() {
		StringBuilder builder = new StringBuilder(64);
		if (id != null) {
			builder.append("id: ").append(id).append('\n');
		}
		if (event != null) {
			builder.append("event: ").append(event).append('\n');
		}
		if (data != null) {
			builder.append("data: ").append(data).append('\n');
		}
		builder.append("\n\n");
		return builder.toString().getBytes(charset);
	}

	public static class Builder {
		private Charset charset = StandardCharsets.UTF_8;
		private String id;
		private String event;
		private String data;

		public Builder charset(Charset charset) {
			this.charset = charset;
			return this;
		}

		public Builder id(String id) {
			this.id = id;
			return this;
		}

		public Builder event(String event) {
			this.event = event;
			return this;
		}

		public Builder data(String data) {
			this.data = data;
			return this;
		}

		public SseChunkedPacket build() {
			return new SseChunkedPacket(this);
		}
	}
}
