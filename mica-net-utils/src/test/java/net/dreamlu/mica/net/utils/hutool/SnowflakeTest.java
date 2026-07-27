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

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Snowflake 单元测试
 *
 * @author L.cm
 */
class SnowflakeTest {

	@Test
	void testNextIdIsPositive() {
		Snowflake snowflake = new Snowflake(0, 0);
		long id = snowflake.nextId();
		assertTrue(id > 0);
	}

	@Test
	void testNextIdUnique() {
		Snowflake snowflake = new Snowflake(1, 1);
		int count = 10000;
		Set<Long> ids = new HashSet<>(count);
		for (int i = 0; i < count; i++) {
			ids.add(snowflake.nextId());
		}
		assertEquals(count, ids.size());
	}

	@Test
	void testNextIdMonotonic() {
		// 单线程下 id 应单调递增
		Snowflake snowflake = new Snowflake(0, 0);
		long previous = snowflake.nextId();
		for (int i = 0; i < 1000; i++) {
			long current = snowflake.nextId();
			assertTrue(current > previous, "id 应该单调递增: " + previous + " -> " + current);
			previous = current;
		}
	}

	@Test
	void testInvalidWorkerId() {
		// workerId 超过 31 (5 位)
		assertThrows(IllegalArgumentException.class, () -> new Snowflake(32, 0));
		assertThrows(IllegalArgumentException.class, () -> new Snowflake(-1, 0));
	}

	@Test
	void testInvalidDatacenterId() {
		// datacenterId 超过 31
		assertThrows(IllegalArgumentException.class, () -> new Snowflake(0, 32));
		assertThrows(IllegalArgumentException.class, () -> new Snowflake(0, -1));
	}

	@Test
	void testMaxValidIds() {
		assertDoesNotThrow(() -> new Snowflake(31, 31));
		assertDoesNotThrow(() -> new Snowflake(0, 0));
	}
}