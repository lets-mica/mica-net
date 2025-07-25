package org.tio.utils.timer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * SystemTimer 调试
 *
 * @author L.cm
 */
public class SystemTimerTest {

	public static void main(String[] args) throws InterruptedException {
		ExecutorService executor = Executors.newCachedThreadPool();
		DefaultTimerTaskService taskService = new DefaultTimerTaskService();
		for (int i = 0; i < 100; i++) {
			executor.submit(taskService::start);
			System.out.println("111");
		}
		executor.awaitTermination(10, TimeUnit.SECONDS);
	}

	public static void main1(String[] args) throws InterruptedException {
		DefaultTimerTaskService taskService = new DefaultTimerTaskService();
		taskService.start();
		taskService.addTask((timer) -> new TimerTask(1, TimeUnit.SECONDS) {
			@Override
			public void run() {
				timer.add(this);
				System.out.println("hello..." + Thread.currentThread().getName());
			}
		});
		TimeUnit.MINUTES.sleep(1L);
		taskService.stop();
	}
}
