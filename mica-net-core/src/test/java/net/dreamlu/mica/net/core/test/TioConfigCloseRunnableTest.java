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

package net.dreamlu.mica.net.core.test;

import net.dreamlu.mica.net.core.task.AbstractCloseRunnable;
import net.dreamlu.mica.net.core.tcp.FixedLengthCodec;
import net.dreamlu.mica.net.core.tcp.TestTioServerHandler;
import net.dreamlu.mica.net.server.DefaultTioServerListener;
import net.dreamlu.mica.net.server.TioServerConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * TioConfig#getCloseRunnable 单例一致性测试。
 *
 * @author L.cm
 */
class TioConfigCloseRunnableTest {

	@Test
	void getCloseRunnableReturnsSameInstanceUnderContention() throws Exception {
		TioServerConfig config = new TioServerConfig(new TestTioServerHandler(new FixedLengthCodec(8)), new DefaultTioServerListener());
		int threads = 32;
		CyclicBarrier barrier = new CyclicBarrier(threads);
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			List<Future<AbstractCloseRunnable>> futures = new ArrayList<>(threads);
			for (int i = 0; i < threads; i++) {
				futures.add(pool.submit(() -> {
					barrier.await(5, TimeUnit.SECONDS);
					return config.getCloseRunnable();
				}));
			}
			AbstractCloseRunnable first = futures.get(0).get(5, TimeUnit.SECONDS);
			Assertions.assertNotNull(first);
			for (Future<AbstractCloseRunnable> future : futures) {
				Assertions.assertSame(first, future.get(5, TimeUnit.SECONDS));
			}
		} finally {
			pool.shutdownNow();
			config.groupExecutor.shutdownNow();
			config.tioExecutor.shutdownNow();
		}
	}
}
