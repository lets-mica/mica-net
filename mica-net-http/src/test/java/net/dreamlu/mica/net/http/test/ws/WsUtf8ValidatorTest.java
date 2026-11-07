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

package net.dreamlu.mica.net.http.test.ws;

import net.dreamlu.mica.net.websocket.common.WsUtf8Validator;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WsUtf8Validator 校验方法语义测试，防止 isValidUtf8 / isInvalidUtf8 的返回值再次反转。
 *
 * @author L.cm
 */
class WsUtf8ValidatorTest {

	@Test
	void isValidUtf8_acceptsLegalUtf8() {
		assertTrue(WsUtf8Validator.isValidUtf8("hello".getBytes(StandardCharsets.UTF_8)));
		// 中文
		assertTrue(WsUtf8Validator.isValidUtf8("中文".getBytes(StandardCharsets.UTF_8)));
		// emoji 是 4 字节字符
		assertTrue(WsUtf8Validator.isValidUtf8("😀".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void isValidUtf8_rejectsIllegalUtf8() {
		// 0xC3 后面跟 0x28 是非法的续字节
		assertFalse(WsUtf8Validator.isValidUtf8(new byte[]{(byte) 0xC3, 0x28}));
		// 孤立的续字节
		assertFalse(WsUtf8Validator.isValidUtf8(new byte[]{(byte) 0x80}));
	}

	@Test
	void isValidUtf8_acceptsEmptyAndNull() {
		assertTrue(WsUtf8Validator.isValidUtf8(new byte[0]));
		assertTrue(WsUtf8Validator.isValidUtf8(null));
	}

	@Test
	void isInvalidUtf8_isTheOppositeOfIsValidUtf8() {
		byte[][] samples = {
			null,
			new byte[0],
			"hello".getBytes(StandardCharsets.UTF_8),
			new byte[]{(byte) 0xC3, 0x28},
			new byte[]{(byte) 0x80}
		};
		for (byte[] sample : samples) {
			assertTrue(WsUtf8Validator.isInvalidUtf8(sample) != WsUtf8Validator.isValidUtf8(sample));
		}
	}
}
