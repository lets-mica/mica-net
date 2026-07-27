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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CollUtil 单元测试
 *
 * @author L.cm
 */
class CollUtilTest {

	@Test
	void testIsEmpty() {
		assertTrue(CollUtil.isEmpty(null));
		assertTrue(CollUtil.isEmpty(new ArrayList<>()));
		assertFalse(CollUtil.isEmpty(Arrays.asList(1, 2, 3)));
	}

	@Test
	void testIsNotEmpty() {
		assertFalse(CollUtil.isNotEmpty(null));
		assertFalse(CollUtil.isNotEmpty(new ArrayList<>()));
		assertTrue(CollUtil.isNotEmpty(Collections.singletonList(1)));
	}

	@Test
	void testComputeIfAbsent() {
		// HashMap 测试
		Map<String, Integer> map = new HashMap<>();
		AtomicInteger counter = new AtomicInteger();
		Integer value = CollUtil.computeIfAbsent(map, "key", k -> counter.incrementAndGet());
		assertEquals(1, value);
		assertEquals(1, map.get("key"));

		// 已存在的 key 不应触发 mappingFunction
		Integer existing = CollUtil.computeIfAbsent(map, "key", k -> {
			fail("不应调用 mappingFunction");
			return 999;
		});
		assertEquals(1, existing);

		// ConcurrentHashMap 测试
		Map<String, String> concurrentMap = new ConcurrentHashMap<>();
		String val = CollUtil.computeIfAbsent(concurrentMap, "k", k -> "computed-" + k);
		assertEquals("computed-k", val);
		assertEquals("computed-k", concurrentMap.get("k"));
	}

	@Test
	void testPartition() {
		List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
		List<List<Integer>> partitions = CollUtil.partition(list, 3);
		assertEquals(3, partitions.size());
		assertEquals(Arrays.asList(1, 2, 3), partitions.get(0));
		assertEquals(Arrays.asList(4, 5, 6), partitions.get(1));
		assertEquals(Collections.singletonList(7), partitions.get(2));
	}

	@Test
	void testPartitionRandomAccess() {
		// ArrayList 实现了 RandomAccess
		List<Integer> arrayList = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
		List<List<Integer>> partitions = CollUtil.partition(arrayList, 2);
		assertEquals(2, partitions.size());
		assertEquals(Arrays.asList(1, 2), partitions.get(0));
		assertEquals(Arrays.asList(3, 4), partitions.get(1));
	}

	@Test
	void testPartitionSingleChunk() {
		List<Integer> list = Arrays.asList(1, 2, 3);
		List<List<Integer>> partitions = CollUtil.partition(list, 10);
		assertEquals(1, partitions.size());
		assertEquals(list, partitions.get(0));
	}

	@Test
	void testPartitionEmptyList() {
		List<List<Integer>> partitions = CollUtil.partition(new ArrayList<>(), 2);
		assertEquals(0, partitions.size());
		assertTrue(partitions.isEmpty());
	}

	@Test
	void testPartitionInvalidSize() {
		assertThrows(IllegalArgumentException.class, () -> CollUtil.partition(Arrays.asList(1, 2), 0));
		assertThrows(IllegalArgumentException.class, () -> CollUtil.partition(Arrays.asList(1, 2), -1));
	}

	@Test
	void testPartitionNullList() {
		assertThrows(NullPointerException.class, () -> CollUtil.partition(null, 2));
	}

	@Test
	void testPartitionOutOfBounds() {
		List<Integer> list = Arrays.asList(1, 2, 3);
		List<List<Integer>> partitions = CollUtil.partition(list, 2);
		assertThrows(IndexOutOfBoundsException.class, () -> partitions.get(99));
		assertThrows(IndexOutOfBoundsException.class, () -> partitions.get(-1));
	}
}
