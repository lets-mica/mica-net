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

package net.dreamlu.mica.net.utils.mica;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IntPair 单元测试
 *
 * @author L.cm
 */
class IntPairTest {

	@Test
	void testGetters() {
		IntPair<String> pair = new IntPair<>(1, "hello");
		assertEquals(1, pair.getKey());
		assertEquals("hello", pair.getValue());
	}

	@Test
	void testEqualsAndHashCode() {
		IntPair<String> pair1 = new IntPair<>(1, "hello");
		IntPair<String> pair2 = new IntPair<>(1, "hello");
		IntPair<String> pair3 = new IntPair<>(2, "hello");
		IntPair<String> pair4 = new IntPair<>(1, "world");

		assertEquals(pair1, pair2);
		assertNotEquals(pair1, pair3);
		assertNotEquals(pair1, pair4);
		assertEquals(pair1.hashCode(), pair2.hashCode());

		assertNotEquals(null, pair1);
		assertNotEquals("not an IntPair", pair1);
	}

	@Test
	void testEqualsWithNullValue() {
		IntPair<String> pair1 = new IntPair<>(1, null);
		IntPair<String> pair2 = new IntPair<>(1, null);
		IntPair<String> pair3 = new IntPair<>(1, "value");
		assertEquals(pair1, pair2);
		assertEquals(pair1.hashCode(), pair2.hashCode());
		assertNotEquals(pair1, pair3);
	}

	@Test
	void testToString() {
		IntPair<String> pair = new IntPair<>(42, "answer");
		String str = pair.toString();
		assertTrue(str.contains("42"));
		assertTrue(str.contains("answer"));
	}
}