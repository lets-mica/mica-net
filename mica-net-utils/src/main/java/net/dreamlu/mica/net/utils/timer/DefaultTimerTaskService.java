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

package net.dreamlu.mica.net.utils.timer;

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

	/**
	 * 默认时间轮粒度（tickMs）。
	 * <p>
	 * 注意：tickMs 不能比业务实际的 {@code delayMs} 大。{@link TimingWheel#add} 对
	 * {@code 过期时刻 < currentTime + tickMs} 的任务直接返回 false，{@link SystemTimer}
	 * 收到 false 后会**立即提交执行**。也就是说，任何 {@code delayMs} 小于一个 tick 的
	 * 任务都会被当成「已到期」而立刻触发，延时退化为 0。
	 * <p>
	 * 老默认值 {@code (1000, 60)} 的粒度就是 1000ms，会让所有亚秒级延时全部变成
	 * 「立即执行」：实测 delay=100ms 触发于 0ms、delay=500ms 触发于 0~389ms。
	 * 客户端重连（{@code reInterval} 常见 100ms）正好落在这个区间，重连间隔会退化成 0
	 * 而形成重连风暴。
	 * <p>
	 * {@code tickMs=10} 与 Kafka {@code tickMs=1} 的判定语义一致（只有亚 10ms 的任务
	 * 才允许立即执行）；{@code wheelSize=100} 让基础轮覆盖 1000ms，使心跳、重连等常见
	 * 延时不必下沉到溢出轮。实测 50ms~5s 的触发偏差均在 10ms 内。
	 */
	public static final long DEFAULT_TICK_MS = 10L;
	/**
	 * 默认每一层时间轮的格数。
	 *
	 * @see #DEFAULT_TICK_MS
	 */
	public static final int DEFAULT_WHEEL_SIZE = 100;

	public DefaultTimerTaskService() {
		this(DEFAULT_TICK_MS, DEFAULT_WHEEL_SIZE);
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
		if (!started.get()) {
			return timerTask;
		}
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
		if (started.compareAndSet(false, true)) {
			timingWheelThread.start();
		}
	}

	@Override
	public void stop() {
		// 先关闭线程池，防止新任务加入
		systemTimer.shutdown();
		timingWheelThread.shutdown();
		started.set(false);
	}

}
