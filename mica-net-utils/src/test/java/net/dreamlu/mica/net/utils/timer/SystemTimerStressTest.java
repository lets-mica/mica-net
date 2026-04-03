package net.dreamlu.mica.net.utils.timer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SystemTimer 压力测试
 * 测试时间轮在高并发、大量任务场景下的性能表现
 *
 * @author L.cm
 */
public class SystemTimerStressTest {

	/**
	 * 测试1: 高并发添加大量任务
	 */
	public static void testConcurrentAddTasks() throws InterruptedException {
		System.out.println("========== 测试1: 高并发添加大量任务 ==========");

		SystemTimer systemTimer = new SystemTimer(100L, 60, "Test1Timer");
		TimingWheelThread timingWheelThread = new TimingWheelThread(systemTimer);
		timingWheelThread.start();

		int threadCount = 50;  // 50个线程
		int tasksPerThread = 10000;  // 每个线程添加10000个任务

		AtomicInteger executedCount = new AtomicInteger(0);
		AtomicLong totalLatency = new AtomicLong(0);

		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch finishLatch = new CountDownLatch(threadCount);

		long testStartTime = System.currentTimeMillis();

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);

		for (int i = 0; i < threadCount; i++) {
			final int threadId = i;
			executor.submit(() -> {
				try {
					startLatch.await(); // 等待统一开始

					Random random = new Random(threadId);
					for (int j = 0; j < tasksPerThread; j++) {
						long delay = random.nextInt(5000) + 100; // 100ms - 5100ms
						// 使用纳秒时间戳，在任务内部记录，避免添加操作的耗时影响

						systemTimer.add(new TimerTask(delay) {
							private final long scheduleTime = System.nanoTime();
							private final long expectedDelay = delay;

							@Override
							public void run() {
								long executeTime = System.nanoTime();
								long actualDelay = (executeTime - scheduleTime) / 1_000_000; // 转换为毫秒
								totalLatency.addAndGet(actualDelay - expectedDelay); // 计算延迟误差
								executedCount.incrementAndGet();
							}
						});
					}

					finishLatch.countDown();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}

		startLatch.countDown(); // 开始测试

		// 等待所有任务添加完成
		boolean finished = finishLatch.await(30, TimeUnit.SECONDS);
		long addEndTime = System.currentTimeMillis();

		if (!finished) {
			System.out.println("任务添加超时！");
		}

		long addDuration = addEndTime - testStartTime;
		int totalTasks = threadCount * tasksPerThread;

		System.out.println("任务添加完成: " + totalTasks + " 个任务");
		System.out.println("添加耗时: " + addDuration + " ms");
		System.out.println("添加吞吐量: " + (totalTasks * 1000L / addDuration) + " 任务/秒");
		System.out.println("时间轮中待执行任务数: " + systemTimer.size());

		// 等待任务执行完成
		System.out.println("等待任务执行...");
		Thread.sleep(10000); // 等待10秒让任务执行

		long executedTaskCount = executedCount.get();
		double avgLatencyError = executedTaskCount > 0 ? (double) totalLatency.get() / executedTaskCount : 0;

		System.out.println("已执行任务数: " + executedTaskCount);
		System.out.println("平均延迟误差: " + String.format("%.2f", avgLatencyError) + " ms");
		System.out.println("执行率: " + String.format("%.2f", (double) executedTaskCount / totalTasks * 100) + "%");

		if (avgLatencyError < 0) {
			System.out.println("注意: 负的延迟误差表示任务平均比预期稍早执行，这在时间轮中是正常的");
			System.out.println("      时间轮的精度为tickMs(100ms)，任务可能在时间格开始时就执行");
		}

		timingWheelThread.shutdown();
		systemTimer.shutdown();
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		System.out.println("测试1完成\n");
	}

	/**
	 * 测试2: 不同延迟时间的任务
	 */
	public static void testVariousDelayTimes() throws InterruptedException {
		System.out.println("========== 测试2: 不同延迟时间的任务 ==========");

		SystemTimer systemTimer = new SystemTimer(10L, 100, "Test2Timer");
		TimingWheelThread timingWheelThread = new TimingWheelThread(systemTimer);
		timingWheelThread.start();

		int[] delays = {10, 50, 100, 500, 1000, 2000, 5000, 10000}; // 不同延迟时间（ms）
		int tasksPerDelay = 1000;

		ConcurrentHashMap<Integer, AtomicInteger> executedMap = new ConcurrentHashMap<>();
		ConcurrentHashMap<Integer, AtomicLong> latencyErrorMap = new ConcurrentHashMap<>();

		for (int delay : delays) {
			executedMap.put(delay, new AtomicInteger(0));
			latencyErrorMap.put(delay, new AtomicLong(0));
		}

		long startTime = System.currentTimeMillis();

		for (int delay : delays) {
			for (int i = 0; i < tasksPerDelay; i++) {
				long addTime = System.currentTimeMillis();
				final int finalDelay = delay;

				systemTimer.add(new TimerTask(finalDelay) {
					@Override
					public void run() {
						long executeTime = System.currentTimeMillis();
						long actualDelay = executeTime - addTime;
						long error = actualDelay - finalDelay;

						executedMap.get(finalDelay).incrementAndGet();
						latencyErrorMap.get(finalDelay).addAndGet(error);
					}
				});
			}
		}

		long addEndTime = System.currentTimeMillis();
		System.out.println("任务添加耗时: " + (addEndTime - startTime) + " ms");

		// 等待所有任务执行完成
		Thread.sleep(15000);

		System.out.println("\n各延迟时间段的执行情况:");
		System.out.println(String.format("%-10s %-15s %-15s %-20s", "延迟(ms)", "执行任务数", "执行率(%)", "平均误差(ms)"));
		System.out.println("----------------------------------------------------------------");

		for (int delay : delays) {
			int executed = executedMap.get(delay).get();
			double rate = (double) executed / tasksPerDelay * 100;
			double avgError = executed > 0 ? (double) latencyErrorMap.get(delay).get() / executed : 0;

			System.out.println(String.format("%-10d %-15d %-15.2f %-20.2f",
				delay, executed, rate, avgError));
		}

		timingWheelThread.shutdown();
		systemTimer.shutdown();
		System.out.println("\n测试2完成\n");
	}

	/**
	 * 测试3: 周期性任务
	 */
	public static void testPeriodicTasks() throws InterruptedException {
		System.out.println("========== 测试3: 周期性任务 ==========");

		SystemTimer systemTimer = new SystemTimer(50L, 100, "Test3Timer");
		TimingWheelThread timingWheelThread = new TimingWheelThread(systemTimer);
		timingWheelThread.start();

		int periodicTaskCount = 100;
		int executionsPerTask = 20; // 每个任务期望执行20次
		long period = 200; // 200ms周期

		AtomicInteger totalExecutions = new AtomicInteger(0);
		ConcurrentHashMap<Integer, AtomicInteger> taskExecutionMap = new ConcurrentHashMap<>();

		for (int i = 0; i < periodicTaskCount; i++) {
			final int taskId = i;
			taskExecutionMap.put(taskId, new AtomicInteger(0));

			systemTimer.add(new TimerTask(period) {
				@Override
				public void run() {
					int execCount = taskExecutionMap.get(taskId).incrementAndGet();
					totalExecutions.incrementAndGet();

					// 如果还没达到期望次数，重新添加自己
					if (execCount < executionsPerTask) {
						systemTimer.add(this);
					}
				}
			});
		}

		// 等待足够长的时间让所有任务都执行完
		long waitTime = period * executionsPerTask + 2000;
		System.out.println("等待 " + waitTime + " ms 让周期性任务执行...");
		Thread.sleep(waitTime);

		int totalExpected = periodicTaskCount * executionsPerTask;
		System.out.println("周期性任务数量: " + periodicTaskCount);
		System.out.println("每个任务期望执行次数: " + executionsPerTask);
		System.out.println("总期望执行次数: " + totalExpected);
		System.out.println("实际总执行次数: " + totalExecutions.get());
		System.out.println("执行率: " + String.format("%.2f", (double) totalExecutions.get() / totalExpected * 100) + "%");

		// 统计执行次数分布
		int[] distribution = new int[executionsPerTask + 1];
		for (AtomicInteger count : taskExecutionMap.values()) {
			int execCount = count.get();
			if (execCount <= executionsPerTask) {
				distribution[execCount]++;
			}
		}

		System.out.println("\n执行次数分布:");
		for (int i = 0; i <= executionsPerTask; i++) {
			if (distribution[i] > 0) {
				System.out.println("执行 " + i + " 次: " + distribution[i] + " 个任务");
			}
		}

		timingWheelThread.shutdown();
		systemTimer.shutdown();
		System.out.println("\n测试3完成\n");
	}

	/**
	 * 测试4: 任务取消
	 */
	public static void testTaskCancellation() throws InterruptedException {
		System.out.println("========== 测试4: 任务取消 ==========");

		SystemTimer systemTimer = new SystemTimer(100L, 60, "Test4Timer");
		TimingWheelThread timingWheelThread = new TimingWheelThread(systemTimer);
		timingWheelThread.start();

		int totalTasks = 10000;
		int cancelRatio = 50; // 取消50%的任务

		AtomicInteger executedCount = new AtomicInteger(0);
		AtomicInteger cancelledCount = new AtomicInteger(0);

		List<TimerTask> tasks = new ArrayList<>();

		// 添加任务
		for (int i = 0; i < totalTasks; i++) {
			TimerTask task = new TimerTask(2000) {
				@Override
				public void run() {
					executedCount.incrementAndGet();
				}
			};
			systemTimer.add(task);
			tasks.add(task);
		}

		System.out.println("已添加 " + totalTasks + " 个任务");
		System.out.println("时间轮中任务数: " + systemTimer.size());

		// 随机取消一定比例的任务
		Random random = new Random();
		for (TimerTask task : tasks) {
			if (random.nextInt(100) < cancelRatio) {
				task.cancel();
				cancelledCount.incrementAndGet();
			}
		}

		System.out.println("已取消 " + cancelledCount.get() + " 个任务");
		System.out.println("取消后时间轮中任务数: " + systemTimer.size());

		// 等待任务执行
		Thread.sleep(5000);

		int expectedExecuted = totalTasks - cancelledCount.get();
		System.out.println("\n期望执行任务数: " + expectedExecuted);
		System.out.println("实际执行任务数: " + executedCount.get());
		System.out.println("误差: " + Math.abs(expectedExecuted - executedCount.get()));

		timingWheelThread.shutdown();
		systemTimer.shutdown();
		System.out.println("\n测试4完成\n");
	}

	/**
	 * 测试5: 极限压力测试 - 百万级任务
	 */
	public static void testMillionTasks() throws InterruptedException {
		System.out.println("========== 测试5: 极限压力测试 - 百万级任务 ==========");

		// 使用更细粒度的时间轮配置
		SystemTimer systemTimer = new SystemTimer(50L, 120, "Test5Timer");
		TimingWheelThread timingWheelThread = new TimingWheelThread(systemTimer);
		timingWheelThread.start();

		int totalTasks = 1000000; // 100万个任务
		int batchSize = 10000;
		int threadCount = 10;

		AtomicInteger executedCount = new AtomicInteger(0);
		AtomicLong minDelay = new AtomicLong(Long.MAX_VALUE);
		AtomicLong maxDelay = new AtomicLong(0);

		long startTime = System.currentTimeMillis();

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(totalTasks / batchSize);

		for (int batch = 0; batch < totalTasks / batchSize; batch++) {
			executor.submit(() -> {
				try {
					Random random = new Random();
					for (int i = 0; i < batchSize; i++) {
						long delay = random.nextInt(3000) + 100; // 100ms - 3100ms
						long addTime = System.currentTimeMillis();

						systemTimer.add(new TimerTask(delay) {
							@Override
							public void run() {
								long executeTime = System.currentTimeMillis();
								long actualDelay = executeTime - addTime;
								long error = actualDelay - delay;

								executedCount.incrementAndGet();

								// 更新最小最大延迟误差
								long currentMin;
								while (error < (currentMin = minDelay.get())) {
									if (minDelay.compareAndSet(currentMin, error)) {
										break;
									}
								}

								long currentMax;
								while (error > (currentMax = maxDelay.get())) {
									if (maxDelay.compareAndSet(currentMax, error)) {
										break;
									}
								}
							}
						});
					}
					latch.countDown();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}

		// 等待所有任务添加完成
		boolean finished = latch.await(60, TimeUnit.SECONDS);
		long addEndTime = System.currentTimeMillis();

		if (!finished) {
			System.out.println("任务添加超时！");
			executor.shutdownNow();
			timingWheelThread.shutdown();
			systemTimer.shutdown();
			return;
		}

		long addDuration = addEndTime - startTime;

		System.out.println("任务添加完成!");
		System.out.println("总任务数: " + totalTasks);
		System.out.println("添加耗时: " + addDuration + " ms");
		System.out.println("添加吞吐量: " + (totalTasks * 1000L / addDuration) + " 任务/秒");
		System.out.println("时间轮中待执行任务数: " + systemTimer.size());

		// 监控执行进度
		System.out.println("\n监控执行进度...");
		for (int i = 0; i < 10; i++) {
			Thread.sleep(2000);
			int executed = executedCount.get();
			System.out.println((i + 1) * 2 + "s: 已执行 " + executed + " 个任务 (" +
				String.format("%.2f", (double) executed / totalTasks * 100) + "%)");
		}

		System.out.println("\n最终统计:");
		System.out.println("总执行任务数: " + executedCount.get());
		System.out.println("执行率: " + String.format("%.2f", (double) executedCount.get() / totalTasks * 100) + "%");
		System.out.println("最小延迟误差: " + minDelay.get() + " ms");
		System.out.println("最大延迟误差: " + maxDelay.get() + " ms");

		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
		timingWheelThread.shutdown();
		systemTimer.shutdown();

		System.out.println("\n测试5完成\n");
	}

	/**
	 * 测试6: 混合场景压力测试
	 */
	public static void testMixedScenario() throws InterruptedException {
		System.out.println("========== 测试6: 混合场景压力测试 ==========");

		SystemTimer systemTimer = new SystemTimer(100L, 60, "Test6Timer");
		TimingWheelThread timingWheelThread = new TimingWheelThread(systemTimer);
		timingWheelThread.start();

		AtomicInteger addedCount = new AtomicInteger(0);  // 统计add()调用次数（包括周期任务的重新添加）
		AtomicInteger executedCount = new AtomicInteger(0);
		AtomicInteger cancelledCount = new AtomicInteger(0);
		AtomicInteger actualCancelledCount = new AtomicInteger(0);  // 实际成功取消的任务数

		AtomicBoolean running = new AtomicBoolean(true);

		ExecutorService executor = Executors.newFixedThreadPool(4);

		// 线程1: 持续添加短延迟任务
		executor.submit(() -> {
			Random random = new Random();
			while (running.get()) {
				try {
					systemTimer.add(new TimerTask(random.nextInt(500) + 50) {
						@Override
						public void run() {
							executedCount.incrementAndGet();
						}
					});
					addedCount.incrementAndGet();
					Thread.sleep(1);
				} catch (InterruptedException e) {
					break;
				}
			}
		});

		// 线程2: 持续添加长延迟任务
		executor.submit(() -> {
			Random random = new Random();
			while (running.get()) {
				try {
					systemTimer.add(new TimerTask(random.nextInt(3000) + 1000) {
						@Override
						public void run() {
							executedCount.incrementAndGet();
						}
					});
					addedCount.incrementAndGet();
					Thread.sleep(5);
				} catch (InterruptedException e) {
					break;
				}
			}
		});

		// 线程3: 持续添加并取消任务
		executor.submit(() -> {
			Random random = new Random();
			while (running.get()) {
				try {
					AtomicBoolean executed = new AtomicBoolean(false);
					TimerTask task = new TimerTask(random.nextInt(2000) + 500) {
						@Override
						public void run() {
							executed.set(true);
							executedCount.incrementAndGet();
						}
					};
					systemTimer.add(task);
					addedCount.incrementAndGet();

					Thread.sleep(10);

					// 50%概率取消
					if (random.nextBoolean()) {
						cancelledCount.incrementAndGet();
						task.cancel();
						// 稍后检查是否真的取消成功
						Thread.sleep(5);
						if (!executed.get()) {
							actualCancelledCount.incrementAndGet();
						}
					}
				} catch (InterruptedException e) {
					break;
				}
			}
		});

		// 线程4: 持续添加周期性任务
		executor.submit(() -> {
			Random random = new Random();
			while (running.get()) {
				try {
					final int maxExecutions = random.nextInt(5) + 3; // 3-7次
					final AtomicInteger execCount = new AtomicInteger(0);

					systemTimer.add(new TimerTask(200) {
						@Override
						public void run() {
							executedCount.incrementAndGet();
							if (execCount.incrementAndGet() < maxExecutions) {
								systemTimer.add(this);
								addedCount.incrementAndGet();  // 统计重新添加
							}
						}
					});
					addedCount.incrementAndGet();  // 统计首次添加

					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			}
		});

		// 运行30秒
		System.out.println("混合场景测试运行30秒...");
		for (int i = 0; i < 30; i++) {
			Thread.sleep(1000);
			System.out.println((i + 1) + "s: 添加=" + addedCount.get() +
				", 执行=" + executedCount.get() +
				", 取消=" + cancelledCount.get() +
				", 队列中=" + systemTimer.size());
		}

		running.set(false);
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		// 等待剩余任务执行完成
		System.out.println("\n等待剩余任务执行...");
		Thread.sleep(5000);

		System.out.println("\n最终统计:");
		System.out.println("总add()调用次数: " + addedCount.get() + " (包括周期任务的重新添加)");
		System.out.println("总执行任务数: " + executedCount.get());
		System.out.println("尝试取消任务数: " + cancelledCount.get());
		System.out.println("实际成功取消数: " + actualCancelledCount.get());
		System.out.println("时间轮中剩余任务数: " + systemTimer.size());
		System.out.println("\n说明: 由于周期性任务会重复执行，执行次数约等于add()调用次数");

		timingWheelThread.shutdown();
		systemTimer.shutdown();
		System.out.println("\n测试6完成\n");
	}

	/**
	 * 运行所有测试
	 */
	public static void runAllTests() throws InterruptedException {
		long overallStartTime = System.currentTimeMillis();

		System.out.println("╔══════════════════════════════════════════════════════════════╗");
		System.out.println("║          SystemTimer 时间轮压力测试套件                      ║");
		System.out.println("╚══════════════════════════════════════════════════════════════╝");
		System.out.println();

		try {
			testConcurrentAddTasks();
			Thread.sleep(2000);

			testVariousDelayTimes();
			Thread.sleep(2000);

			testPeriodicTasks();
			Thread.sleep(2000);

			testTaskCancellation();
			Thread.sleep(2000);

			// 注意：百万级任务测试比较耗时，可以根据需要启用
			// testMillionTasks();
			// Thread.sleep(2000);

			testMixedScenario();

		} catch (Exception e) {
			System.err.println("测试过程中发生错误: " + e.getMessage());
			e.printStackTrace();
		}

		long overallEndTime = System.currentTimeMillis();
		long totalDuration = overallEndTime - overallStartTime;

		System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
		System.out.println("║          所有测试完成                                         ║");
		System.out.println("║          总耗时: " + String.format("%-46s", totalDuration + " ms") + "║");
		System.out.println("╚══════════════════════════════════════════════════════════════╝");
	}

	public static void main(String[] args) throws InterruptedException {
		// 运行所有测试
		runAllTests();
		// 或者运行单个测试
//		testConcurrentAddTasks();
//		testVariousDelayTimes();
//		testPeriodicTasks();
//		testTaskCancellation();
//		testMillionTasks();
//		testMixedScenario();
	}
}

