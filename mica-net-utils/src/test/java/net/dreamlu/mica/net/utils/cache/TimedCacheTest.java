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

package net.dreamlu.mica.net.utils.cache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class TimedCacheTest {

	/**
	 * 过期语义：默认过期时长、自定义过期时长、访问刷新、永不过期。
	 * <p>
	 * 过期时长按秒级取，避免毫秒级断言受线程调度抖动影响而偶发失败。
	 */
	@Test
	void timedCacheTest() throws InterruptedException, IOException {
		// 默认过期 1 秒
		try (TimedCache<String, String> timedCache = new TimedCache<>(TimeUnit.SECONDS.toMillis(1))) {
			timedCache.put("key1", "value1", 1);//1毫秒过期
			timedCache.put("key2", "value2", TimeUnit.SECONDS.toMillis(5));//5秒过期
			timedCache.put("key3", "value3");//默认过期(1秒)
			timedCache.put("key4", "value4", Long.MAX_VALUE);//永不过期

			//200毫秒后 key3 仍在，并刷新过期时间
			Thread.sleep(200);
			String value3 = timedCache.getAndRefresh("key3");
			//刷新后再等200毫秒，仍未过期
			Thread.sleep(200);
			Assertions.assertNotNull(value3);

			//key1 已过期，key2 未过期
			String value1 = timedCache.get("key1");
			Assertions.assertNull(value1);
			String value2 = timedCache.get("key2");
			Assertions.assertEquals("value2", value2);

			//距上次刷新已超过 1 秒，key3 过期
			Thread.sleep(1000);
			value3 = timedCache.get("key3");
			Assertions.assertNull(value3);

			String value3Supplier = timedCache.get("key3", () -> "Default supplier");
			Assertions.assertEquals("Default supplier", value3Supplier);

			// 永不过期
			String value4 = timedCache.get("key4");
			Assertions.assertEquals("value4", value4);

			//取消定时清理
			timedCache.cancelPruneSchedule();
		}
	}

	/**
	 * 定时清理必须在持锁状态下执行。
	 * <p>
	 * 定时清理会删除底层 map 中的元素，若不持锁，会与业务线程的读写并发，
	 * 触发 {@link java.util.ConcurrentModificationException}；
	 * 且该异常抛在定时线程上被 Future 静默吞掉，业务侧完全无感知。
	 */
	@Test
	void scheduledPruneShouldHoldLock() throws Exception {
		AtomicBoolean locked = new AtomicBoolean(false);
		try (TimedCache<String, String> timedCache = new TimedCache<String, String>(100) {
			@Override
			protected int pruneCache() {
				locked.set(lock.isHeldByCurrentThread());
				return super.pruneCache();
			}
		}) {
			timedCache.put("key", "value", 50);
			// 清理周期兜底为秒级，最多等 3 秒
			long deadline = System.currentTimeMillis() + 3000;
			while (!locked.get() && System.currentTimeMillis() < deadline) {
				Thread.sleep(50);
			}
			Assertions.assertTrue(locked.get(), "定时清理必须在持锁状态下执行");
		}
	}

}
