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

package net.dreamlu.mica.net.core.context;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.tcp.FixedLengthCodec;
import net.dreamlu.mica.net.core.tcp.TestTioServerHandler;
import net.dreamlu.mica.net.server.DefaultTioServerListener;
import net.dreamlu.mica.net.server.ServerChannelContext;
import net.dreamlu.mica.net.server.TioServerConfig;

/**
 * ChannelContext state bit tests.
 *
 * @author L.cm
 */
class ChannelContextStateTest {

	@Test
	void testStateStorageUsesAtomicInteger() throws Exception {
		Field statesField = ChannelContext.class.getDeclaredField("states");
		Assertions.assertEquals(AtomicInteger.class, statesField.getType());
		Assertions.assertTrue(Modifier.isFinal(statesField.getModifiers()));
	}

	@Test
	void testConcurrentStateBitUpdatesDoNotOverwriteOtherBits() throws Exception {
		int contextSize = 128;
		int iterations = 10000;
		ServerChannelContext[] contexts = newContexts(contextSize);
		CyclicBarrier startBarrier = new CyclicBarrier(5);
		CyclicBarrier endBarrier = new CyclicBarrier(5);
		ExecutorService executor = Executors.newFixedThreadPool(4);
		List<Future<?>> futures = Arrays.asList(
			executor.submit(() -> {
				runStateSetter(iterations, startBarrier, endBarrier, contexts, context -> context.setClosed(true));
				return null;
			}),
			executor.submit(() -> {
				runStateSetter(iterations, startBarrier, endBarrier, contexts, context -> context.setWaitingClose(true));
				return null;
			}),
			executor.submit(() -> {
				runStateSetter(iterations, startBarrier, endBarrier, contexts, context -> context.setRemoved(true));
				return null;
			}),
			executor.submit(() -> {
				runStateSetter(iterations, startBarrier, endBarrier, contexts, context -> context.setAccepted(false));
				return null;
			})
		);

		try {
			for (int i = 0; i < iterations; i++) {
				for (ServerChannelContext context : contexts) {
					context.setClosed(false);
					context.setWaitingClose(false);
					context.setRemoved(false);
					context.setAccepted(true);
					Assertions.assertTrue(context.isAccepted());
				}

				startBarrier.await();
				endBarrier.await();

				for (int j = 0; j < contexts.length; j++) {
					ServerChannelContext context = contexts[j];
					Assertions.assertTrue(context.isClosed(), "closed bit lost at iteration " + i + ", context " + j);
					Assertions.assertTrue(context.isWaitingClose(), "waitingClose bit lost at iteration " + i + ", context " + j);
					Assertions.assertTrue(context.isRemoved(), "removed bit lost at iteration " + i + ", context " + j);
					context.setClosed(false);
					Assertions.assertFalse(context.isAccepted(), "accepted bit restored at iteration " + i + ", context " + j);
				}
			}
		} finally {
			executor.shutdownNow();
		}

		for (Future<?> future : futures) {
			future.get();
		}
	}

	private ServerChannelContext[] newContexts(int size) {
		TioServerConfig config = new TioServerConfig(new TestTioServerHandler(new FixedLengthCodec(8)), new DefaultTioServerListener());
		ServerChannelContext[] contexts = new ServerChannelContext[size];
		for (int i = 0; i < contexts.length; i++) {
			contexts[i] = new ServerChannelContext(config, "state-test-" + i);
		}
		return contexts;
	}

	private void runStateSetter(int iterations, CyclicBarrier startBarrier, CyclicBarrier endBarrier,
	                            ServerChannelContext[] contexts, StateSetter setter) throws Exception {
		for (int i = 0; i < iterations && !Thread.currentThread().isInterrupted(); i++) {
			startBarrier.await();
			for (ServerChannelContext context : contexts) {
				setter.set(context);
			}
			endBarrier.await();
		}
	}

	@FunctionalInterface
	private interface StateSetter {
		void set(ServerChannelContext context);
	}

}
