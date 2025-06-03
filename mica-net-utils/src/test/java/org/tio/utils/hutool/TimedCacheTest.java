package org.tio.utils.hutool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tio.utils.cache.TimedCache;

import java.util.concurrent.TimeUnit;

class TimedCacheTest {


	@Test
	void timedCacheTest() throws InterruptedException {
		TimedCache<String, String> timedCache = new TimedCache<>(4);
		timedCache.put("key1", "value1", 1);//1毫秒过期
		timedCache.put("key2", "value2", TimeUnit.SECONDS.toMillis(5));//5秒过期
		timedCache.put("key3", "value3");//默认过期(4毫秒)
		timedCache.put("key4", "value4", Long.MAX_VALUE);//永不过期

		//等待5毫秒
		Thread.sleep(2);
		String value3 = timedCache.getAndRefresh("key3");
		//等待5毫秒
		Thread.sleep(3);
		Assertions.assertNotNull(value3);

		//5毫秒后由于value2设置了5毫秒过期，因此只有value2被保留下来
		String value1 = timedCache.get("key1");
		Assertions.assertNull(value1);
		String value2 = timedCache.get("key2");
		Assertions.assertEquals("value2", value2);

		Thread.sleep(2);
		//5毫秒后，由于设置了默认过期，key3只被保留4毫秒，因此为null
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
