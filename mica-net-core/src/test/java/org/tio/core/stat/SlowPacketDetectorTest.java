package org.tio.core.stat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 慢包攻击检测器单元测试
 *
 * @author mica-net
 * 2026-02-11
 */
public class SlowPacketDetectorTest {

	@Test
	public void testBasicRecording() {
		SlowPacketDetector detector = new SlowPacketDetector(5, 1);

		// 记录5次接收
		detector.recordReceive(100);
		detector.recordReceive(200);
		detector.recordReceive(150);
		detector.recordReceive(250);
		detector.recordReceive(300);

		// 验证平均值
		assertEquals(200, detector.getAverageBytes(), "平均值应为 (100+200+150+250+300)/5 = 200");
		assertEquals(1000, detector.getTotalBytes(), "总字节数应为 1000");
		assertEquals(5, detector.getSampleCount(), "样本数应为 5");
	}

	@Test
	public void testSlidingWindow() {
		SlowPacketDetector detector = new SlowPacketDetector(3, 1);

		// 记录超过窗口大小的数据
		detector.recordReceive(100);
		detector.recordReceive(200);
		detector.recordReceive(300);
		assertEquals(200, detector.getAverageBytes(), "平均值应为 200");

		// 添加第4个值，应该覆盖第1个值(100)
		detector.recordReceive(400);
		assertEquals(300, detector.getAverageBytes(), "平均值应为 (200+300+400)/3 = 300");

		// 添加第5个值，应该覆盖第2个值(200)
		detector.recordReceive(500);
		assertEquals(400, detector.getAverageBytes(), "平均值应为 (300+400+500)/3 = 400");
	}

	@Test
	public void testCheckInterval() {
		SlowPacketDetector detector = new SlowPacketDetector(10, 5);

		// 前4次失败不应触发检测
		assertFalse(detector.shouldCheck(1), "失败1次不应检测");
		assertFalse(detector.shouldCheck(2), "失败2次不应检测");
		assertFalse(detector.shouldCheck(3), "失败3次不应检测");
		assertFalse(detector.shouldCheck(4), "失败4次不应检测");

		// 第5次失败应触发检测
		assertTrue(detector.shouldCheck(5), "失败5次应触发检测");

		// 下一次检测应在第10次
		assertFalse(detector.shouldCheck(6), "失败6次不应检测");
		assertFalse(detector.shouldCheck(9), "失败9次不应检测");
		assertTrue(detector.shouldCheck(10), "失败10次应触发检测");
	}

	@Test
	public void testCheckIntervalOne() {
		SlowPacketDetector detector = new SlowPacketDetector(10, 1);

		// 间隔为1时，每次都应检测
		assertTrue(detector.shouldCheck(1), "间隔为1时每次都应检测");
		assertTrue(detector.shouldCheck(2), "间隔为1时每次都应检测");
		assertTrue(detector.shouldCheck(100), "间隔为1时每次都应检测");
	}

	@Test
	public void testReset() {
		SlowPacketDetector detector = new SlowPacketDetector(5, 1);

		detector.recordReceive(100);
		detector.recordReceive(200);
		detector.recordReceive(300);

		assertEquals(200, detector.getAverageBytes());
		assertEquals(3, detector.getSampleCount());

		// 重置后应恢复初始状态
		detector.reset();
		assertEquals(0, detector.getAverageBytes(), "重置后平均值应为0");
		assertEquals(0, detector.getTotalBytes(), "重置后总字节数应为0");
		assertEquals(0, detector.getSampleCount(), "重置后样本数应为0");
	}

	@Test
	public void testEmptyWindow() {
		SlowPacketDetector detector = new SlowPacketDetector(10, 1);

		// 空窗口应返回0
		assertEquals(0, detector.getAverageBytes(), "空窗口平均值应为0");
		assertEquals(0, detector.getTotalBytes(), "空窗口总字节数应为0");
		assertEquals(0, detector.getSampleCount(), "空窗口样本数应为0");
	}

	@Test
	public void testSlowAttackScenario() {
		// 模拟慢包攻击场景：每次只发送很少的字节
		SlowPacketDetector detector = new SlowPacketDetector(16, 1);

		for (int i = 0; i < 20; i++) {
			detector.recordReceive(10); // 每次只发10字节
		}

		// 平均值应该很低，表明可能是慢包攻击
		assertEquals(10, detector.getAverageBytes(), "慢包攻击场景下平均值应很低");
		assertTrue(detector.getAverageBytes() < 128, "平均值应低于阈值，表明可能是攻击");
	}

	@Test
	public void testNormalScenario() {
		// 模拟正常场景：正常大小的数据包
		SlowPacketDetector detector = new SlowPacketDetector(16, 1);

		for (int i = 0; i < 20; i++) {
			detector.recordReceive(1024 + (i * 10)); // 正常大小的包
		}

		// 平均值应该正常
		assertTrue(detector.getAverageBytes() > 1000, "正常场景下平均值应该较高");
	}

	@Test
	public void testConfigurationBounds() {
		// 测试边界条件
		SlowPacketDetector detector1 = new SlowPacketDetector(0, 0);
		assertEquals(1, detector1.getWindowSize(), "窗口大小最小应为1");
		assertEquals(1, detector1.getCheckInterval(), "检测间隔最小应为1");

		SlowPacketDetector detector2 = new SlowPacketDetector(-5, -10);
		assertEquals(1, detector2.getWindowSize(), "负数窗口大小应调整为1");
		assertEquals(1, detector2.getCheckInterval(), "负数检测间隔应调整为1");
	}

	@Test
	public void testDefaultConfiguration() {
		SlowPacketDetector detector = new SlowPacketDetector();

		assertEquals(16, detector.getWindowSize(), "默认窗口大小应为16");
		assertEquals(1, detector.getCheckInterval(), "默认检测间隔应为1");
	}

	@Test
	public void testHighFrequencyRecording() {
		// 测试高频记录场景
		SlowPacketDetector detector = new SlowPacketDetector(100, 10);

		for (int i = 0; i < 1000; i++) {
			detector.recordReceive(i % 500);
		}

		// 验证不会溢出或出错
		assertTrue(detector.getSampleCount() <= 100, "样本数不应超过窗口大小");
		assertTrue(detector.getAverageBytes() >= 0, "平均值应为非负数");
	}

	@Test
	public void testMixedPacketSizes() {
		// 测试混合大小的数据包
		SlowPacketDetector detector = new SlowPacketDetector(10, 1);

		detector.recordReceive(10);
		detector.recordReceive(1000);
		detector.recordReceive(50);
		detector.recordReceive(2000);
		detector.recordReceive(100);

		int avg = detector.getAverageBytes();
		assertTrue(avg > 0, "平均值应大于0");
		assertEquals(3160 / 5, avg, "平均值计算应正确");
	}
}
