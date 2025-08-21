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

package org.tio.utils.mica;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 {@link StrTemplateParser}
 *
 * @author L.cm
 */
class StrTemplateParserTest {

	@Test
	void testParseTemplateAndGetVariables_NormalCase() {
		StrTemplateParser parser = new StrTemplateParser("abc${name}xx${like}cc");
		Map<String, String> result = parser.getVariables("abcDreamluxxHellocc");
		assertEquals(2, result.size());
		assertEquals("Dreamlu", result.get("name"));
		assertEquals("Hello", result.get("like"));
	}

	@Test
	void testParseTemplateAndGetVariables_EmptyTemplate() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new StrTemplateParser(null);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new StrTemplateParser("");
		});
	}

	@Test
	void testParseTemplateAndGetVariables_NoVariables() {
		StrTemplateParser parser = new StrTemplateParser("abc");
		Map<String, String> result = parser.getVariables("abc");
		assertTrue(result.isEmpty());
	}

	@Test
	void testParseTemplateAndGetVariables_MismatchLiteral() {
		StrTemplateParser parser = new StrTemplateParser("abc${name}xx");
		Map<String, String> result = parser.getVariables("abxDreamluxx");
		assertTrue(result.isEmpty());
	}

	@Test
	void testParseTemplateAndGetVariables_EmptyVariableValue() {
		StrTemplateParser parser = new StrTemplateParser("a${x}b${y}c");
		Map<String, String> result = parser.getVariables("abc");
		assertEquals("", result.get("x"));
		assertEquals("", result.get("y"));
	}

	@Test
	void testParseTemplate_InvalidTemplate_MissingEnd() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			new StrTemplateParser("abc${name");
		});
		assertTrue(exception.getMessage().contains("缺少结束符"));
	}

	@Test
	void testParseTemplate_InvalidTemplate_EmptyVariableName() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			new StrTemplateParser("abc${}xx");
		});
		assertTrue(exception.getMessage().contains("变量名为空"));
	}
}
