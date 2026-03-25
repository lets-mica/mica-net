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

import org.tio.utils.hutool.StrUtil;

/**
 * Server-Sent Events 数据
 * <p>
 * 用于构建 SSE 事件，支持 builder 模式
 *
 * @author L.cm
 */
public class SseData {
	private String id;
	private String event;
	private String comment;
	private Object data;

	private SseData() {
	}

	public static SseData of(Object data) {
		return new SseData().data(data);
	}

	public static SseData of(String event, Object data) {
		return new SseData().event(event).data(data);
	}

	public static SseData of(String id, String event, Object data) {
		return new SseData().id(id).event(event).data(data);
	}

	public static SseData of(long id, String event, Object data) {
		return new SseData().id(id).event(event).data(data);
	}

	/**
	 * 添加 SSE "id" 行
	 */
	public SseData id(long id) {
		this.id = String.valueOf(id);
		return this;
	}

	/**
	 * 添加 SSE "id" 行
	 */
	public SseData id(String id) {
		this.id = id;
		return this;
	}

	/**
	 * 添加 SSE "event" 行
	 */
	public SseData event(String event) {
		this.event = event;
		return this;
	}

	/**
	 * 添加 SSE "comment" 行
	 */
	public SseData comment(String comment) {
		this.comment = comment;
		return this;
	}

	/**
	 * 添加 SSE "data" 行
	 */
	public SseData data(Object data) {
		this.data = data;
		return this;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String CRLF = "\r\n";
		if (id != null) {
			sb.append("id: ").append(id).append(CRLF);
		}
		if (event != null) {
			sb.append("event: ").append(event).append(CRLF);
		}
		if (comment != null) {
			sb.append(": ").append(StrUtil.replace(comment, "\n", "\n: ")).append(CRLF);
		}
		if (data != null) {
			String dataStr = String.valueOf(data);
			if (dataStr.contains("\n")) {
				sb.append("data: ").append(dataStr.replace("\n", "\ndata: ")).append(CRLF);
			} else {
				sb.append("data: ").append(dataStr).append(CRLF);
			}
		}
		sb.append(CRLF);
		return sb.toString();
	}
}
