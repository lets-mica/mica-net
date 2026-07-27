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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StrUtil 单元测试
 *
 * @author L.cm
 */
class StrUtilTest {

	@Test
	void testIsEmpty() {
		assertTrue(StrUtil.isEmpty(null));
		assertTrue(StrUtil.isEmpty(""));
		assertFalse(StrUtil.isEmpty(" "));
		assertFalse(StrUtil.isEmpty("a"));
	}

	@Test
	void testIsBlank() {
		assertTrue(StrUtil.isBlank(null));
		assertTrue(StrUtil.isBlank(""));
		assertTrue(StrUtil.isBlank(" "));
		assertTrue(StrUtil.isBlank("\t\n"));
		assertFalse(StrUtil.isBlank("a"));
		assertFalse(StrUtil.isBlank(" a "));
	}

	@Test
	void testIsNotBlank() {
		assertFalse(StrUtil.isNotBlank(null));
		assertFalse(StrUtil.isNotBlank(""));
		assertTrue(StrUtil.isNotBlank("a"));
	}

	@Test
	void testEquals() {
		assertTrue(StrUtil.equals(null, null));
		assertFalse(StrUtil.equals(null, "a"));
		assertFalse(StrUtil.equals("a", null));
		assertTrue(StrUtil.equals("abc", "abc"));
		assertFalse(StrUtil.equals("abc", "ABC"));
		assertTrue(StrUtil.equalsIgnoreCase("abc", "ABC"));
	}

	@Test
	void testReplace() {
		assertEquals("hello world", StrUtil.replace("hello world", "x", "y"));
		assertEquals("hi world", StrUtil.replace("hello world", "hello", "hi"));
		// 多次替换
		assertEquals("a+b+c", StrUtil.replace("a-b-c", "-", "+"));
		// 每次都把 - 替换为 a，结果为 aaaaaaaaa（5 个 a + 4 个新 a）
		assertEquals("aaaaaaaaa", StrUtil.replace("a-a-a-a-a", "-", "a"));
		// null 安全
		assertNull(StrUtil.replace(null, "a", "b"));
		assertEquals("abc", StrUtil.replace("abc", "", "x"));
		assertEquals("abc", StrUtil.replace("abc", "a", null));
	}

	@Test
	void testStartWithEndWith() {
		assertTrue(StrUtil.startWith("hello", "he"));
		assertFalse(StrUtil.startWith("hello", "HE"));
		assertTrue(StrUtil.startWithIgnoreCase("hello", "HE"));
		assertFalse(StrUtil.startWith("hello", "lo"));
		assertTrue(StrUtil.startWith(null, null));
		assertFalse(StrUtil.startWith(null, "a"));
		assertFalse(StrUtil.startWith("a", null));

		assertTrue(StrUtil.endWith("hello", "lo"));
		assertFalse(StrUtil.endWith("hello", "LO"));
		assertTrue(StrUtil.endWithIgnoreCase("hello", "LO"));
		assertFalse(StrUtil.endWith("hello", "he"));
		assertTrue(StrUtil.endWith(null, null));
		assertFalse(StrUtil.endWith(null, "a"));
		assertFalse(StrUtil.endWith("a", null));

		// 单字符
		assertTrue(StrUtil.startWith("hello", 'h'));
		assertFalse(StrUtil.startWith("hello", 'H'));
		assertTrue(StrUtil.endWith("hello", 'o'));
		assertFalse(StrUtil.endWith("hello", 'O'));
	}

	@Test
	void testIndexOf() {
		assertEquals(2, StrUtil.indexOf("hello", 'l'));
		assertEquals(-1, StrUtil.indexOf("hello", 'z'));
		assertEquals(2, StrUtil.indexOf("hello", 'l', 0));
		assertEquals(3, StrUtil.indexOf("hello", 'l', 3));
		assertEquals(-1, StrUtil.indexOf("hello", 'l', 5));
	}

	@Test
	void testIndexOfIgnoreCase() {
		assertEquals(0, StrUtil.indexOfIgnoreCase("aabaabaa", "A"));
		assertEquals(1, StrUtil.indexOfIgnoreCase("aabaabaa", "AB"));
		assertEquals(-1, StrUtil.indexOfIgnoreCase("aabaabaa", "Z"));
		assertEquals(-1, StrUtil.indexOfIgnoreCase(null, "A"));
		assertEquals(-1, StrUtil.indexOfIgnoreCase("abc", null));
	}

	@Test
	void testLastIndexOfIgnoreCase() {
		assertEquals(5, StrUtil.lastIndexOfIgnoreCase("aabaabaa", "B"));
		assertEquals(-1, StrUtil.lastIndexOfIgnoreCase("abc", "Z"));
	}

	@Test
	void testContains() {
		assertTrue(StrUtil.contains("hello", 'h'));
		assertFalse(StrUtil.contains("hello", 'z'));
		assertTrue(StrUtil.containsIgnoreCase("Hello", "he"));
		assertFalse(StrUtil.containsIgnoreCase("Hello", "x"));
		assertTrue(StrUtil.containsIgnoreCase(null, null));
		assertFalse(StrUtil.containsIgnoreCase(null, "abc"));
	}

	@Test
	void testSplit() {
		String[] parts = StrUtil.split("a,b,c", ",");
		assertArrayEquals(new String[]{"a", "b", "c"}, parts);

		assertNull(StrUtil.split(null, ","));
	}

	@Test
	void testJoin() {
		List<String> list = Arrays.asList("a", "b", "c");
		assertEquals("a,b,c", StrUtil.join(list));
		assertEquals("a-b-c", StrUtil.join(list, "-"));
		assertEquals("a;b;c", StrUtil.join(list, ";"));

		// 空集合
		assertEquals("", StrUtil.join(new ArrayList<>()));
		assertEquals("", StrUtil.join(null));
	}

	@Test
	void testSub() {
		assertEquals("he", StrUtil.sub("hello", 0, 2));
		assertEquals("llo", StrUtil.sub("hello", -3, 5));
		assertEquals("el", StrUtil.sub("hello", -4, -2));
		assertEquals("hello", StrUtil.sub("hello", 0, 100));
		// from > to 时交换
		assertEquals("he", StrUtil.sub("hello", 2, 0));
		// from == to 返回空
		assertEquals("", StrUtil.sub("hello", 2, 2));
		assertNull(StrUtil.sub(null, 0, 1));
		// 空串返回空串
		assertEquals("", StrUtil.sub("", 0, 1));
	}

	@Test
	void testSubPre() {
		assertEquals("he", StrUtil.subPre("hello", 2));
		assertEquals("hello", StrUtil.subPre("hello", 100));
	}

	@Test
	void testSubSuf() {
		assertEquals("llo", StrUtil.subSuf("hello", 2));
		assertNull(StrUtil.subSuf(null, 0));
		assertNull(StrUtil.subSuf("", 0));
	}

	@Test
	void testSubSufByLength() {
		assertEquals("cde", StrUtil.subSufByLength("abcde", 3));
		assertEquals("", StrUtil.subSufByLength("abcde", 0));
		assertEquals("", StrUtil.subSufByLength("abcde", -1));
		assertEquals("abcde", StrUtil.subSufByLength("abcde", 10));
		assertNull(StrUtil.subSufByLength(null, 3));
	}

	@Test
	void testSubBetween() {
		assertEquals("b", StrUtil.subBetween("wx[b]yz", "[", "]"));
		assertEquals("abc", StrUtil.subBetween("yabcz", "y", "z"));
		assertNull(StrUtil.subBetween(null, "a", "b"));
		assertNull(StrUtil.subBetween("abc", null, "b"));
		assertNull(StrUtil.subBetween("abc", "a", null));
		// 不存在
		assertNull(StrUtil.subBetween("abc", "[", "]"));
	}

	@Test
	void testSubAfter() {
		assertEquals("bc", StrUtil.subAfter("abc", "a", false));
		assertEquals("cba", StrUtil.subAfter("abcba", "b", false));
		// 最后一个 'b' 之后是 'a'
		assertEquals("a", StrUtil.subAfter("abcba", "b", true));
		assertEquals("abc", StrUtil.subAfter("abc", "", false));
		assertEquals("", StrUtil.subAfter("abc", "d", false));
	}

	@Test
	void testSubBefore() {
		assertEquals("", StrUtil.subBefore("abc", "a", false));
		assertEquals("a", StrUtil.subBefore("abcba", "b", false));
		assertEquals("abc", StrUtil.subBefore("abcba", "b", true));
		assertEquals("abc", StrUtil.subBefore("abc", "d", false));
	}

	@Test
	void testTrim() {
		assertNull(StrUtil.trim(null));
		assertEquals("abc", StrUtil.trim("abc"));
		assertEquals("abc", StrUtil.trim("  abc  "));
		assertEquals("abc", StrUtil.trimStart("  abc"));
		// trimStart 只修剪头部
		assertEquals("abc  ", StrUtil.trimStart("abc  "));
		assertEquals("abc", StrUtil.trimEnd("abc  "));
		// trimEnd 只修剪尾部
		assertEquals("  abc", StrUtil.trimEnd("  abc"));
	}

	@Test
	void testUpperFirstLowerFirst() {
		assertEquals("Hello", StrUtil.upperFirst("hello"));
		assertEquals("Hello", StrUtil.upperFirst("Hello"));
		// 首字母小写会被转为大写
		assertEquals("HELLO", StrUtil.upperFirst("hELLO"));
		assertNull(StrUtil.upperFirst(null));

		assertEquals("hello", StrUtil.lowerFirst("Hello"));
		assertEquals("hello", StrUtil.lowerFirst("hello"));
		assertNull(StrUtil.lowerFirst(null));
	}

	@Test
	void testMaxLength() {
		assertEquals("hello", StrUtil.maxLength("hello", 10));
		assertEquals("hello...", StrUtil.maxLength("hello world", 5));
		assertNull(StrUtil.maxLength(null, 5));
	}

	@Test
	void testContainsAny() {
		assertTrue(StrUtil.containsAny("hello world", "foo", "world"));
		assertFalse(StrUtil.containsAny("hello world", "foo", "bar"));
		assertNull(StrUtil.getContainsStr(null, "foo"));
		assertNull(StrUtil.getContainsStr("hello", (CharSequence[]) null));
		assertEquals("world", StrUtil.getContainsStr("hello world", "foo", "world"));
	}

	@Test
	void testGetUUId() {
		String uuid = StrUtil.getUUId();
		assertNotNull(uuid);
		assertEquals(32, uuid.length());
		// 多次调用返回不同的 id
		assertNotEquals(uuid, StrUtil.getUUId());
	}

	@Test
	void testGetNanoId() {
		String nanoId = StrUtil.getNanoId();
		assertNotNull(nanoId);
		assertEquals(21, nanoId.length());
		assertNotEquals(nanoId, StrUtil.getNanoId());
	}

	@Test
	void testGetIdLenTooShort() {
		assertThrows(IllegalArgumentException.class, () -> StrUtil.getId(new Random(), 7, 16));
	}

	@Test
	void testGetIdLength() {
		String id = StrUtil.getId(new Random(1), 10, 36);
		assertEquals(10, id.length());
	}

	@Test
	void testFill() {
		assertEquals("abcxx", StrUtil.fillAfter("abc", 'x', 5));
		assertEquals("xxabc", StrUtil.fill("abc", 'x', 5, true));
		// 长度不够直接返回
		assertEquals("abc", StrUtil.fillAfter("abc", 'x', 2));
	}

	@Test
	void testRepeat() {
		assertEquals("aaa", StrUtil.repeat('a', 3));
		assertEquals("", StrUtil.repeat('a', 0));
		assertEquals("", StrUtil.repeat('a', -1));
	}

	@Test
	void testConvert() throws Exception {
		assertEquals(Integer.valueOf(123), StrUtil.convert(Integer.class, "123"));
		assertEquals(Long.valueOf(123L), StrUtil.convert(Long.class, "123"));
		assertEquals(Boolean.TRUE, StrUtil.convert(Boolean.class, "true"));
		assertEquals(Boolean.TRUE, StrUtil.convert(Boolean.class, "1"));
		assertEquals(Boolean.TRUE, StrUtil.convert(Boolean.class, "yes"));
		assertEquals(Boolean.FALSE, StrUtil.convert(Boolean.class, "false"));
		assertEquals(Double.valueOf(1.23), StrUtil.convert(Double.class, "1.23"));
		assertEquals("abc", StrUtil.convert(String.class, "abc"));
		// blank -> null
		assertNull(StrUtil.convert(Integer.class, ""));
		assertNull(StrUtil.convert(Integer.class, "  "));
	}

	@Test
	void testConvertArrays() throws Exception {
		int[] intArr = (int[]) StrUtil.convert(int.class, new String[]{"1", "2", "3"});
		assertArrayEquals(new int[]{1, 2, 3}, intArr);

		long[] longArr = (long[]) StrUtil.convert(long.class, new String[]{"1", "2"});
		assertArrayEquals(new long[]{1L, 2L}, longArr);

		String[] strArr = (String[]) StrUtil.convert(String.class, new String[]{"a", "b"});
		assertArrayEquals(new String[]{"a", "b"}, strArr);

		assertNull(StrUtil.convert(int.class, (String[]) null));
	}

	@Test
	void testConvertUnknownType() {
		assertThrows(Exception.class, () -> StrUtil.convert(java.io.File.class, "x"));
	}

	@Test
	void testStr() {
		assertNull(StrUtil.str(null));
		assertEquals("abc", StrUtil.str("abc"));
		assertEquals("[1, 2, 3]", StrUtil.str(new int[]{1, 2, 3}, StandardCharsets.UTF_8));
		assertEquals("abc", StrUtil.str(new StringBuilder("abc"), StandardCharsets.UTF_8));
	}

	@Test
	void testUtf8Str() {
		assertEquals("hello", StrUtil.utf8Str("hello"));
	}
}