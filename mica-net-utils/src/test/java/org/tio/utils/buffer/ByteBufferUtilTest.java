package org.tio.utils.buffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * ByteBufferUtil 单元测试
 */
class ByteBufferUtilTest {

	@Test
	void test1() {
		short short1 = 25511;
		short short2 = -25511;
		ByteBuffer byteBuffer = ByteBuffer.allocate(28);
		// short
		byteBuffer.putShort(short1);
		ByteBufferUtil.writeShortLE(byteBuffer, short2);
		// int
		int int1 = 25511111;
		int int2 = -25511111;
		byteBuffer.putInt(int1);
		ByteBufferUtil.writeIntLE(byteBuffer, int2);
		// long
		long long1 = 255111111111L;
		long long2 = -255111111111L;
		byteBuffer.putLong(long1);
		ByteBufferUtil.writeLongLE(byteBuffer, long2);
		byteBuffer.flip();
		Assertions.assertEquals(short1, ByteBufferUtil.readShortBE(byteBuffer));
		Assertions.assertEquals(short2, ByteBufferUtil.readShortLE(byteBuffer));
		Assertions.assertEquals(int1, ByteBufferUtil.readIntBE(byteBuffer));
		Assertions.assertEquals(int2, ByteBufferUtil.readIntLE(byteBuffer));
		Assertions.assertEquals(long1, ByteBufferUtil.readLongBE(byteBuffer));
		Assertions.assertEquals(long2, ByteBufferUtil.readLongLE(byteBuffer));
	}

	@Test
	void test2() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(24);
		// float
		float float1 = 25511111.0001F;
		float float2 = -25511111.0002F;
		byteBuffer.putFloat(float1);
		ByteBufferUtil.writeFloatLE(byteBuffer, float2);
		// double
		double double1 = 255111111111.011111D;
		double double2 = -255111111111.02222D;
		byteBuffer.putDouble(double1);
		ByteBufferUtil.writeDoubleLE(byteBuffer, double2);
		byteBuffer.flip();
		Assertions.assertEquals(float1, ByteBufferUtil.readFloatBE(byteBuffer));
		Assertions.assertEquals(float2, ByteBufferUtil.readFloatLE(byteBuffer));
		Assertions.assertEquals(double1, ByteBufferUtil.readDoubleBE(byteBuffer));
		Assertions.assertEquals(double2, ByteBufferUtil.readDoubleLE(byteBuffer));
	}

	@Test
	void testShort() {
		int value = 65535;
		ByteBuffer buffer = ByteBuffer.allocate(6);
		ByteBufferUtil.writeShortLE(buffer, value);
		ByteBufferUtil.writeIntBE(buffer, value);
		buffer.flip();
		int unsignedShort = ByteBufferUtil.readUnsignedShortLE(buffer);
		Assertions.assertEquals(value, unsignedShort);
		int shortValue = ByteBufferUtil.readIntBE(buffer);
		Assertions.assertEquals(value, shortValue);
	}

	@Test
	void testShortBE() {
		int value = 65535;
		ByteBuffer buffer = ByteBuffer.allocate(6);
		ByteBufferUtil.writeShortBE(buffer, value);
		ByteBufferUtil.writeIntLE(buffer, value);
		buffer.flip();
		int unsignedShort = ByteBufferUtil.readUnsignedShortBE(buffer);
		Assertions.assertEquals(value, unsignedShort);
		int shortValue = ByteBufferUtil.readIntLE(buffer);
		Assertions.assertEquals(value, shortValue);
	}

	@Test
	void testMedium() {
		int value = 16777215;
		ByteBuffer buffer = ByteBuffer.allocate(3);
		ByteBufferUtil.writeMediumLE(buffer, value);
		buffer.flip();
		int unsignedShort = ByteBufferUtil.readUnsignedMediumLE(buffer);
		Assertions.assertEquals(value, unsignedShort);
	}

	@Test
	void testMediumBE() {
		int value = 16777215;
		ByteBuffer buffer = ByteBuffer.allocate(3);
		ByteBufferUtil.writeMediumBE(buffer, value);
		buffer.flip();
		int unsignedShort = ByteBufferUtil.readUnsignedMediumBE(buffer);
		Assertions.assertEquals(value, unsignedShort);
	}

	@Test
	void testMediumSignedLE() {
		// 有符号medium，小端 -1
		int value = -1;
		ByteBuffer buffer = ByteBuffer.allocate(3);
		ByteBufferUtil.writeMediumLE(buffer, value);
		buffer.flip();
		int result = ByteBufferUtil.readMediumLE(buffer);
		Assertions.assertEquals(value, result);
	}

	@Test
	void testMediumSignedBE() {
		// 有符号medium，大端 -1
		int value = -1;
		ByteBuffer buffer = ByteBuffer.allocate(3);
		ByteBufferUtil.writeMediumBE(buffer, value);
		buffer.flip();
		int result = ByteBufferUtil.readMediumBE(buffer);
		Assertions.assertEquals(value, result);
	}

	@Test
	void testInt() {
		long value = 4294967295L;
		ByteBuffer buffer = ByteBuffer.allocate(12);
		ByteBufferUtil.writeIntLE(buffer, value);
		ByteBufferUtil.writeLongBE(buffer, value);
		buffer.flip();
		long unsignedShort = ByteBufferUtil.readUnsignedIntLE(buffer);
		Assertions.assertEquals(value, unsignedShort);
		long intValue = ByteBufferUtil.readLongBE(buffer);
		Assertions.assertEquals(value, intValue);
	}

	@Test
	void testIntBE() {
		long value = 4294967295L;
		ByteBuffer buffer = ByteBuffer.allocate(12);
		ByteBufferUtil.writeIntBE(buffer, value);
		ByteBufferUtil.writeLongLE(buffer, value);
		buffer.flip();
		long unsignedShort = ByteBufferUtil.readUnsignedIntBE(buffer);
		Assertions.assertEquals(value, unsignedShort);
		long intValue = ByteBufferUtil.readLongLE(buffer);
		Assertions.assertEquals(value, intValue);
	}

	@Test
	void testNB() {
		int test = 123456789;
		ByteBuffer buffer = ByteBuffer.allocate(100);
		ByteBufferUtil.writeIntBE(buffer, test);
		buffer.flip();
		long byteBE = ByteBufferUtil.readUnsignedNByteBE(buffer, 4);
		Assertions.assertEquals(test, byteBE);
		buffer.clear();
		ByteBufferUtil.writeIntLE(buffer, test);
		buffer.flip();
		long byteLE = ByteBufferUtil.readUnsignedNByteLE(buffer, 4);
		Assertions.assertEquals(test, byteLE);
	}

	@Test
	void testHexDumpHeapBuffer() {
		// 堆缓冲区测试：hexDump不消耗position
		byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.allocate(100);
		buffer.position(10);
		buffer.put(data);
		buffer.position(10);
		buffer.limit(10 + data.length);

		int posBefore = buffer.position();
		int limitBefore = buffer.limit();
		String hex = ByteBufferUtil.hexDump(buffer);
		Assertions.assertEquals(posBefore, buffer.position());
		Assertions.assertEquals(limitBefore, buffer.limit());
		Assertions.assertTrue(hex.contains("68 65 6c 6c 6f")); // "hello" hex
	}

	@Test
	void testHexDumpDirectBuffer() {
		// 直接缓冲区测试：hexDump不消耗position
		byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.allocateDirect(100);
		buffer.position(10);
		buffer.put(data);
		buffer.position(10);
		buffer.limit(10 + data.length);

		int posBefore = buffer.position();
		int limitBefore = buffer.limit();
		String hex = ByteBufferUtil.hexDump(buffer);
		Assertions.assertEquals(posBefore, buffer.position());
		Assertions.assertEquals(limitBefore, buffer.limit());
		Assertions.assertTrue(hex.contains("68 65 6c 6c 6f"));
	}

	@Test
	void testToStringHeapBuffer() {
		// 堆缓冲区测试：toString不消耗position
		byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.allocate(100);
		buffer.position(5);
		buffer.put(data);
		buffer.position(5);
		buffer.limit(5 + data.length);

		int posBefore = buffer.position();
		int limitBefore = buffer.limit();
		String str = ByteBufferUtil.toString(buffer, StandardCharsets.UTF_8);
		Assertions.assertEquals(posBefore, buffer.position());
		Assertions.assertEquals(limitBefore, buffer.limit());
		Assertions.assertEquals("hello", str);
	}

	@Test
	void testToStringDirectBuffer() {
		// 直接缓冲区测试：toString不消耗position
		byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.allocateDirect(100);
		buffer.position(5);
		buffer.put(data);
		buffer.position(5);
		buffer.limit(5 + data.length);

		int posBefore = buffer.position();
		int limitBefore = buffer.limit();
		String str = ByteBufferUtil.toString(buffer, StandardCharsets.UTF_8);
		Assertions.assertEquals(posBefore, buffer.position());
		Assertions.assertEquals(limitBefore, buffer.limit());
		Assertions.assertEquals("hello", str);
	}

	@Test
	void testCopyHeapBuffer() {
		// 堆缓冲区 copy 测试
		byte[] srcData = "hello world".getBytes(StandardCharsets.UTF_8);
		ByteBuffer src = ByteBuffer.allocate(100);
		src.put(srcData);

		ByteBuffer dst = ByteBuffer.allocate(100);
		ByteBufferUtil.copy(src, 0, dst, 0, 5);

		dst.position(0);
		dst.limit(5);
		Assertions.assertEquals("hello", ByteBufferUtil.toString(dst, StandardCharsets.UTF_8));
	}

	@Test
	void testCopyDirectBuffer() {
		// 直接缓冲区 copy 测试
		byte[] srcData = "hello world".getBytes(StandardCharsets.UTF_8);
		ByteBuffer src = ByteBuffer.allocateDirect(100);
		src.put(srcData);

		ByteBuffer dest = ByteBuffer.allocateDirect(100);
		ByteBufferUtil.copy(src, 0, dest, 0, 5);

		dest.position(0);
		dest.limit(5);
		Assertions.assertEquals("hello", ByteBufferUtil.toString(dest, StandardCharsets.UTF_8));
	}

	@Test
	void testIndexOfFound() {
		// 找到字符的情况
		byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.wrap(data);
		Assertions.assertEquals(4, ByteBufferUtil.indexOf(buffer, 'o', 20));
	}

	@Test
	void testIndexOfNotFoundWithinLimit() {
		// 在 maxLength 内未找到，抛出异常
		byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.wrap(data);
		Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
			ByteBufferUtil.indexOf(buffer, 'x', 5); // 只搜索前5个字节
		});
	}

	@Test
	void testIndexOfNotFoundWithinRemaining() {
		// remaining < maxLength，未找到返回-1
		byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.wrap(data);
		buffer.position(0);
		buffer.limit(5); // 只读前5个字节
		Assertions.assertEquals(-1, ByteBufferUtil.indexOf(buffer, 'x', 100)); // maxLength > remaining
	}

}
