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

package org.tio.utils.timer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Default TimerTaskService
 *
 * @author L.cm
 */
public class DefaultTimerTaskService implements TimerTaskService {
	private final SystemTimer systemTimer;
	private final TimingWheelThread timingWheelThread;
	private final AtomicBoolean started = new AtomicBoolean(false);

	public DefaultTimerTaskService() {
		this(1000L, 60);
	}

	public DefaultTimerTaskService(long tickMs, int wheelSize) {
		this(new SystemTimer(tickMs, wheelSize, "DefaultTimerTaskService"));
	}

	public DefaultTimerTaskService(SystemTimer systemTimer) {
		this(systemTimer, new TimingWheelThread(systemTimer));
	}

	public DefaultTimerTaskService(SystemTimer systemTimer, TimingWheelThread timingWheelThread) {
		this.systemTimer = systemTimer;
		this.timingWheelThread = timingWheelThread;
	}

	@Override
	public <T extends TimerTask> T add(T timerTask) {
		this.systemTimer.add(timerTask);
		return timerTask;
	}

	@Override
	public <T extends TimerTask> T addTask(Function<SystemTimer, T> consumer) {
		return this.add(consumer.apply(this.systemTimer));
	}

	@Override
	public void start() {
		// 确保多次调用只启动一次
		if (started.compareAndSet(false, true) && !timingWheelThread.isStarted()) {
			timingWheelThread.start();
		}
	}

	@Override
	public void stop() {
		started.set(false);
		timingWheelThread.shutdown();
		systemTimer.shutdown();
	}

}
