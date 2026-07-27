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

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ArrayUtil 单元测试
 *
 * @author L.cm
 */
class ArrayUtilTest {

	@Test
	void testContains() {
		String[] array = {"a", "b", "c"};
		assertTrue(ArrayUtil.contains(array, "a"));
		assertTrue(ArrayUtil.contains(array, "c"));
		assertFalse(ArrayUtil.contains(array, "d"));
		assertFalse(ArrayUtil.contains(array, null));
		// null 数组
		assertFalse(ArrayUtil.contains(null, "a"));
		// null 值在 null 数组中视为不存在
		String[] arrayWithNull = {"a", null};
		assertTrue(ArrayUtil.contains(arrayWithNull, null));
	}

	@Test
	void testIndexOf() {
		String[] array = {"a", "b", "c", "b"};
		assertEquals(0, ArrayUtil.indexOf(array, "a"));
		assertEquals(1, ArrayUtil.indexOf(array, "b"));
		assertEquals(2, ArrayUtil.indexOf(array, "c"));
		assertEquals(-1, ArrayUtil.indexOf(array, "z"));
		assertEquals(-1, ArrayUtil.indexOf(null, "a"));
	}

	@Test
	void testJoin() {
		String[] array = {"a", "b", "c"};
		assertEquals("a,b,c", ArrayUtil.join(array, ","));
		assertEquals("a-b-c", ArrayUtil.join(array, "-"));
		assertEquals("abc", ArrayUtil.join(array, ""));

		// 单元素数组
		String[] single = {"only"};
		assertEquals("only", ArrayUtil.join(single, ","));

		// 空数组
		String[] empty = new String[0];
		assertEquals("", ArrayUtil.join(empty, ","));

		// null 数组
		assertNull(ArrayUtil.join(null, ","));
	}

	@Test
	void testIsArray() {
		assertTrue(ArrayUtil.isArray(new int[]{1, 2, 3}));
		assertTrue(ArrayUtil.isArray(new String[]{"a"}));
		assertTrue(ArrayUtil.isArray(new Object[0]));
		assertFalse(ArrayUtil.isArray("string"));
		assertFalse(ArrayUtil.isArray(123));
		assertFalse(ArrayUtil.isArray(null));
	}

	@Test
	void testToStringObjectArray() {
		Object[] array = {"a", "b", "c"};
		String str = ArrayUtil.toString(array);
		assertEquals("[a, b, c]", str);
	}

	@Test
	void testToStringPrimitiveArrays() {
		assertEquals("[1, 2, 3]", ArrayUtil.toString(new int[]{1, 2, 3}));
		assertEquals("[1, 2, 3]", ArrayUtil.toString(new long[]{1L, 2L, 3L}));
		assertEquals("[1.0, 2.0]", ArrayUtil.toString(new double[]{1.0, 2.0}));
		assertEquals("[a, b]", ArrayUtil.toString(new char[]{'a', 'b'}));
		assertEquals("[true, false]", ArrayUtil.toString(new boolean[]{true, false}));
		assertEquals("[1, 2]", ArrayUtil.toString(new byte[]{1, 2}));
	}

	@Test
	void testToStringNull() {
		assertNull(ArrayUtil.toString(null));
	}

	@Test
	void testFilter() {
		Integer[] array = {1, 2, 3, 4, 5};
		Predicate<Integer> predicate = i -> i > 2;
		Integer[] filtered = ArrayUtil.filter(array, predicate);
		assertArrayEquals(new Integer[]{3, 4, 5}, filtered);
	}

	@Test
	void testFilterUnaryOperator() {
		String[] array = {"abc", "de", "fghi"};
		// 替换超过2个字符的字符串为大写
		UnaryOperator<String> editor = s -> s.length() > 2 ? s.toUpperCase() : null;
		String[] filtered = ArrayUtil.filter(array, editor);
		// null 会被过滤掉
		assertArrayEquals(new String[]{"ABC", "FGHI"}, filtered);
	}

	@Test
	void testNewArray() {
		String[] arr = ArrayUtil.newArray(String.class, 5);
		assertEquals(5, arr.length);
		assertNotNull(arr);
	}

	@Test
	void testAddAllBytes() {
		byte[] a = {1, 2};
		byte[] b = {3, 4};
		byte[] c = {5, 6};
		byte[] result = ArrayUtil.addAll(a, b, c);
		assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6}, result);

		// 单个数组
		assertArrayEquals(a, ArrayUtil.addAll(a));

		// 包含 null
		assertArrayEquals(new byte[]{1, 2, 5, 6}, ArrayUtil.addAll(a, null, c));

		// 全 null
		assertArrayEquals(new byte[0], ArrayUtil.addAll(null, null));
	}
}