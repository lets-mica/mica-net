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

/**
 * sse 报文
 *
 * @author L.cm
 */
public class SseEvent {
	private final StringBuilder buf = new StringBuilder();

	/**
	 * 添加 SSE "id" 行.
	 */
	public SseEvent id(long id) {
		buf.append("id:").append(id).append("\n");
		return this;
	}

	/**
	 * 添加 SSE "id" 行.
	 */
	public SseEvent id(String id) {
		buf.append("id:").append(id).append("\n");
		return this;
	}

	/**
	 * 添加 SSE "event" 行.
	 */
	public SseEvent name(String event) {
		buf.append("event:").append(event).append("\n");
		return this;
	}

	/**
	 * 添加 SSE "retry" 行.
	 */
	public SseEvent reconnectTime(long reconnectTimeMillis) {
		buf.append("retry:").append(reconnectTimeMillis).append("\n");
		return this;
	}

	/**
	 * 添加 SSE "data" 行.
	 */
	public SseEvent data(Object object) {
		if (object instanceof String) {
			buf.append("data:").append(object).append("\n");
		} else {
			buf.append("data:").append(object.toString()).append("\n");
		}
		return this;
	}

	/**
	 * 添加 SSE "comment" 行.
	 */
	public SseEvent comment(String comment) {
		buf.append(":").append(comment.replace("\n", "\n:")).append("\n");
		return this;
	}

	@Override
	public String toString() {
		// 最后一行必须以 \n\n 结尾，补充一个 \n
		return buf.append("\n").toString();
	}

}
