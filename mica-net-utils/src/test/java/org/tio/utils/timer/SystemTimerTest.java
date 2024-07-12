package org.tio.utils.timer;

import java.util.concurrent.TimeUnit;

/**
 * SystemTimer 调试
 *
 * @author L.cm
 */
public class SystemTimerTest {

	public static void main(String[] args) throws InterruptedException {
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
