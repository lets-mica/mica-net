package org.tio.utils.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.utils.Threads;
import org.tio.utils.json.JsonUtil;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 文件队列测试
 *
 * @author L.cm
 */
public class FileQueueTest {
	private static final Logger log = LoggerFactory.getLogger(FileQueueTest.class);
	public static String DEFAULT_ROOT = System.getProperty("user.dir") + "/mica-net-utils/target";

	public static void main(String[] args) throws Exception {
		FileQueue<Test> queue = FileQueue.builder()
			.path(Paths.get(DEFAULT_ROOT, "test-nio"))
			.maxFileSize(500 * 1024 * 1024)
			.build();
		parallelWrit1bw(queue);
		TimeUnit.SECONDS.sleep(3);
		parallelRead1bw(queue);
//        testISO8601();
	}

	static void testISO8601() throws IOException {
		FileQueue<Duration[]> queue = FileQueue.builder()
			.path(Paths.get(DEFAULT_ROOT, "test-0"))
			.build();
		ThreadPoolExecutor executor = Threads.getGroupExecutor(4);
		for (int i = 0; i < executor.getCorePoolSize(); i++) {
			executor.execute(() -> {
				while (true) {
					Duration[] msg = new Duration[]{random(), random(), random()};
					queue.put(msg, JsonUtil::toJsonBytes);
					log.info("生产者：" + Arrays.toString(msg));
					try {
						TimeUnit.SECONDS.sleep(5);
					} catch (InterruptedException e) {
						return;
					}
				}
			});
		}
		// 单线程读
		new Thread(() -> {
			while (true) {
				try {
					Duration[] msg = queue.take(bytes -> JsonUtil.readValue(bytes, Duration[].class));
					log.info("消费者：" + Arrays.toString(msg));
				} catch (InterruptedException e) {
					return;
				}

			}
		}).start();
	}

	static Duration random() {
		ChronoUnit[] a = new ChronoUnit[]{ChronoUnit.HOURS, ChronoUnit.MINUTES, ChronoUnit.SECONDS, ChronoUnit.MILLIS};
		return Duration.of(Math.round(8), a[Math.round(a.length)]);
	}

	private static void parallelWrit1bw(FileQueue<Test> queue) {
		for (int i = 0; i < 1; i++) {
			final int a = i;
			new Thread(() -> {
				int count = 10000000;
				long start = System.currentTimeMillis();
				try {
					for (int j = 0; j < count; j++) {
						Test test = new Test(a + "name" + j, null);
						queue.put(test, JsonUtil::toJsonBytes);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				long end = System.currentTimeMillis();
				System.out.println("生产:" + count + "条,数据耗时:" + (end - start) + "毫秒");
			}).start();
		}
	}

	private static void parallelRead1bw(FileQueue<Test> queue) {
		for (int i = 0; i < 2; i++) {
			final int a = i;
			new Thread(() -> {
				int count = 0;
				long start = System.currentTimeMillis();
				try {
					while (queue.poll(bytes -> JsonUtil.readValue(bytes, Test.class)) != null) {
						count++;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				long end = System.currentTimeMillis();
				System.out.println("线程:" + a + ",消费:" + count + "条,数据耗时:" + (end - start) + "毫秒");
			}).start();
		}
	}

	public static class Test {

		private String name;

		private List<Test1> test1s;

		public Test() {
		}

		public Test(String name, List<Test1> test1s) {
			this.name = name;
			this.test1s = test1s;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Test1> getTest1s() {
			return test1s;
		}

		public void setTest1s(List<Test1> test1s) {
			this.test1s = test1s;
		}
	}

	public static class Test1 {
		public Test1() {
		}

		private int id;

		public Test1(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return "Test1{" +
				"id=" + id +
				'}';
		}
	}
}
