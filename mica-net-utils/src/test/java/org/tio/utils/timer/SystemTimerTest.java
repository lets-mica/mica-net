package org.tio.utils.timer;

import java.util.concurrent.TimeUnit;

/**
 * SystemTimer 调试
 *
 * @author L.cm
 */
public class SystemTimerTest {

	public static void main(String[] args) throws InterruptedException {
		SystemTimer systemTimer = new SystemTimer("timer");

		TimingWheelThread timingWheelThread = new TimingWheelThread(systemTimer);
		timingWheelThread.start();

		System.out.println(System.currentTimeMillis());
		systemTimer.add(new TimerTask(5) {
			@Override
			public void run() {
				systemTimer.add(this);
				System.out.println("hello...");
			}
		});
		System.out.println(System.nanoTime());
		TimeUnit.MINUTES.sleep(10L);
	}
}
