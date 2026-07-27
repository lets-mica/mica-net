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
 * Pair 单元测试
 *
 * @author L.cm
 */
class PairTest {

	@Test
	void testGetters() {
		Pair<String, Integer> pair = new Pair<>("hello", 100);
		assertEquals("hello", pair.getLeft());
		assertEquals(Integer.valueOf(100), pair.getRight());
	}

	@Test
	void testEqualsAndHashCode() {
		Pair<String, Integer> pair1 = new Pair<>("key", 1);
		Pair<String, Integer> pair2 = new Pair<>("key", 1);
		Pair<String, Integer> pair3 = new Pair<>("key", 2);
		Pair<String, Integer> pair4 = new Pair<>("other", 1);

		// 相等性
		assertEquals(pair1, pair2);
		assertNotEquals(pair1, pair3);
		assertNotEquals(pair1, pair4);

		// 哈希码一致
		assertEquals(pair1.hashCode(), pair2.hashCode());

		// null 与不同类型比较
		assertNotEquals(null, pair1);
		assertNotEquals("not a pair", pair1);
	}

	@Test
	void testEqualsWithNullFields() {
		Pair<String, Integer> pair1 = new Pair<>(null, null);
		Pair<String, Integer> pair2 = new Pair<>(null, null);
		Pair<String, Integer> pair3 = new Pair<>("x", null);
		assertEquals(pair1, pair2);
		assertEquals(pair1.hashCode(), pair2.hashCode());
		assertNotEquals(pair1, pair3);
	}

	@Test
	void testToString() {
		Pair<String, Integer> pair = new Pair<>("hello", 100);
		String str = pair.toString();
		assertTrue(str.contains("hello"));
		assertTrue(str.contains("100"));
	}
}