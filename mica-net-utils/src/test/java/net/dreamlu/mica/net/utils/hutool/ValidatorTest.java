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

package net.dreamlu.mica.net.utils.hutool;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validator 单元测试
 *
 * @author L.cm
 */
class ValidatorTest {

	@Test
	void testIsNumber() {
		// 整数
		assertTrue(Validator.isNumber("123"));
		assertTrue(Validator.isNumber("-123"));
		// 注意：Validator 不识别字符串开头的 '+'
		assertFalse(Validator.isNumber("+123"));

		// 小数
		assertTrue(Validator.isNumber("1.23"));
		assertTrue(Validator.isNumber("-1.23"));

		// 指数
		assertTrue(Validator.isNumber("1.23e10"));
		assertTrue(Validator.isNumber("1.23E10"));
		assertTrue(Validator.isNumber("1.23e-10"));

		// 十六进制（仅识别 0x 前缀，不识别 0X）
		assertTrue(Validator.isNumber("0x1A"));
		assertFalse(Validator.isNumber("0XFF"));
		assertTrue(Validator.isNumber("0xabc"));

		// 类型后缀
		assertTrue(Validator.isNumber("123L"));
		assertTrue(Validator.isNumber("1.0d"));
		assertTrue(Validator.isNumber("1.0f"));

		// null/空白
		assertFalse(Validator.isNumber(null));
		assertFalse(Validator.isNumber(""));
		assertFalse(Validator.isNumber("   "));
	}

	@Test
	void testIsNumberInvalid() {
		assertFalse(Validator.isNumber("abc"));
		assertFalse(Validator.isNumber("12a"));
		assertFalse(Validator.isNumber("1.2.3"));
		assertFalse(Validator.isNumber("--1"));
		assertFalse(Validator.isNumber("+"));
		assertFalse(Validator.isNumber("0x"));
		assertFalse(Validator.isNumber("0xG"));
		assertFalse(Validator.isNumber("1E"));
		assertFalse(Validator.isNumber("1E1.0"));
	}

	@Test
	void testIsMatch() {
		Pattern digit = Pattern.compile("\\d+");
		assertTrue(Validator.isMatch(digit, "12345"));
		assertFalse(Validator.isMatch(digit, "abc"));
		assertFalse(Validator.isMatch(digit, null));
		assertFalse(Validator.isMatch(null, "abc"));
		assertFalse(Validator.isMatch(null, null));
	}
}