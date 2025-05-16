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
import org.tio.core.Tio;

/**
 * 默认的 sse 发射器
 *
 * @author L.cm
 */
public class SseDefaultEmitter implements SseEmitter<SsePacket> {
	private final ChannelContext context;

	public SseDefaultEmitter(ChannelContext context) {
		this.context = context;
	}

	@Override
	public void push(SsePacket packet) {
		Tio.send(context, packet);
	}
}
