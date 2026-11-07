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

package net.dreamlu.mica.net.websocket.common;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * UTF-8 合法性校验工具。
 *
 * <p>RFC 6455 §5.6 规定：TEXT 帧的负载必须是有效的 UTF-8 编码，
 * Close 帧中携带的原因（reason）同样必须是有效的 UTF-8 编码。
 * 分片消息的 UTF-8 校验必须在所有分片拼接完成之后进行，单个分片可能是半个多字节字符。</p>
 *
 * @author L.cm
 */
public final class WsUtf8Validator {

	private WsUtf8Validator() {
	}

	/**
	 * 判断字节数组是否是合法的 UTF-8 序列
	 *
	 * @param bytes 待校验的字节数组
	 * @return true 表示是合法的 UTF-8
	 */
	public static boolean isValidUtf8(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return false;
		}
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
			.onMalformedInput(CodingErrorAction.REPORT)
			.onUnmappableCharacter(CodingErrorAction.REPORT);
		try {
			decoder.decode(ByteBuffer.wrap(bytes));
			return false;
		} catch (CharacterCodingException e) {
			return true;
		}
	}
}
