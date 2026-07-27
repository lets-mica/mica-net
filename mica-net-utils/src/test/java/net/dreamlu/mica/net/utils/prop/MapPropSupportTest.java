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

package net.dreamlu.mica.net.utils.prop;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MapPropSupport 单元测试
 *
 * @author L.cm
 */
class MapPropSupportTest {

	@Test
	void testSetAndGet() {
		MapPropSupport support = new MapPropSupport();
		support.set("name", "mica");
		support.set("age", 10);
		assertEquals("mica", support.get("name"));
		assertEquals(Integer.valueOf(10), support.get("age"));
		assertNull(support.get("notExists"));
	}

	@Test
	void testContainsKey() {
		MapPropSupport support = new MapPropSupport();
		assertFalse(support.containsKey("k"));
		support.set("k", "v");
		assertTrue(support.containsKey("k"));
	}

	@Test
	void testRemove() {
		MapPropSupport support = new MapPropSupport();
		support.set("k", "v");
		assertTrue(support.containsKey("k"));
		support.remove("k");
		assertFalse(support.containsKey("k"));
		assertNull(support.get("k"));
	}

	@Test
	void testClear() {
		MapPropSupport support = new MapPropSupport();
		support.set("k1", "v1");
		support.set("k2", "v2");
		support.clear();
		assertFalse(support.containsKey("k1"));
		assertFalse(support.containsKey("k2"));
	}

	@Test
	void testComputeIfAbsent() {
		MapPropSupport support = new MapPropSupport();
		AtomicInteger counter = new AtomicInteger();
		String value = support.computeIfAbsent("key", k -> {
			counter.incrementAndGet();
			return "computed";
		});
		assertEquals("computed", value);
		assertEquals(1, counter.get());
		assertEquals("computed", support.get("key"));

		// 已存在时不应触发
		String existing = support.computeIfAbsent("key", k -> {
			counter.incrementAndGet();
			return "should-not-happen";
		});
		assertEquals("computed", existing);
		assertEquals(1, counter.get());
	}

	@Test
	void testGetWithMapper() {
		MapPropSupport support = new MapPropSupport();
		support.set("count", 42);
		// 通过 mapper 转换类型
		String formatted = support.get("count", v -> "value-" + v);
		assertEquals("value-42", formatted);

		// 缺失的 key 时 mapper 仍会被调用，传入 null
		String missing = support.get("missing", v -> "x");
		assertEquals("x", missing);
	}

	@Test
	void testGetAndRemove() {
		MapPropSupport support = new MapPropSupport();
		support.set("k", "v");
		String value = support.getAndRemove("k");
		assertEquals("v", value);
		assertFalse(support.containsKey("k"));

		// 第二次获取返回 null
		assertNull(support.getAndRemove("k"));
	}

	@Test
	void testGetAndRemoveWithMapper() {
		MapPropSupport support = new MapPropSupport();
		support.set("n", 100);
		Integer value = support.getAndRemove("n", v -> ((Number) v).intValue() * 2);
		assertEquals(200, value);
		assertFalse(support.containsKey("n"));
	}

	@Test
	void testOverwriteValue() {
		MapPropSupport support = new MapPropSupport();
		support.set("k", "v1");
		support.set("k", "v2");
		assertEquals("v2", support.get("k"));
	}
}