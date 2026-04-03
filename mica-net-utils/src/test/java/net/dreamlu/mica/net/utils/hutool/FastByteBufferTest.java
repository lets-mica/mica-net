package net.dreamlu.mica.net.utils.hutool;

import net.dreamlu.mica.net.utils.buffer.ByteBufferUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * FastByteBuffer 测试
 *
 * @author L.cm
 */
class FastByteBufferTest {

	@Test
	void test1() {
		FastByteBuffer buffer = new FastByteBuffer();
		short short1 = 25511;
		buffer.writeShortBE(short1);
		short short2 = -25511;
		buffer.writeShortLE(short2);
		int int1 = 25511111;
		buffer.writeIntBE(int1);
		int int2 = -25511111;
		buffer.writeIntLE(int2);
		long long1 = 255111111111L;
		buffer.writeLongBE(long1);
		long long2 = -255111111111L;
		buffer.writeLongLE(long2);
		ByteBuffer byteBuffer = buffer.toBuffer();
		Assertions.assertEquals(short1, ByteBufferUtil.readShortBE(byteBuffer));
		Assertions.assertEquals(short2, ByteBufferUtil.readShortLE(byteBuffer));
		Assertions.assertEquals(int1, ByteBufferUtil.readIntBE(byteBuffer));
		Assertions.assertEquals(int2, ByteBufferUtil.readIntLE(byteBuffer));
		Assertions.assertEquals(long1, ByteBufferUtil.readLongBE(byteBuffer));
		Assertions.assertEquals(long2, ByteBufferUtil.readLongLE(byteBuffer));
	}

	@Test
	void test2() {
		FastByteBuffer buffer = new FastByteBuffer(16);
		byte[] bytes1 = new byte[16];
		buffer.append(bytes1);
		buffer.setBytes(0, new byte[]{1, 1});
		byte[] bytes2 = new byte[16];
		buffer.append(bytes2);
		byte[] bytes3 = new byte[16];
		buffer.append(bytes3);
		byte[] bytes4 = new byte[32];
		Arrays.fill(bytes4, (byte) 8);
		buffer.setBytes(12, bytes4);
		ByteBuffer byteBuffer = buffer.toBuffer();
		byte[] bytes = new byte[32];
		ByteBufferUtil.skipBytes(byteBuffer, 12);
		byteBuffer.get(bytes);
		Assertions.assertArrayEquals(bytes, bytes4);
	}

	@Test
	void test3() {
		FastByteBuffer buffer1 = new FastByteBuffer(32);
		buffer1.append(new byte[32]);
		buffer1.setIntBE(0, 1024);
		buffer1.setLongBE(4, 1024);
		ByteBuffer byteBuffer = buffer1.toBuffer();
		int i = ByteBufferUtil.readIntBE(byteBuffer);
		long l = ByteBufferUtil.readLongBE(byteBuffer);
		Assertions.assertEquals(i, 1024);
		Assertions.assertEquals(l, 1024);
		buffer1.setDoubleBE(12, 1024.1024);
		byteBuffer = buffer1.toBuffer();
		ByteBufferUtil.skipBytes(byteBuffer, 12);
		double v = ByteBufferUtil.readDoubleBE(byteBuffer);
		Assertions.assertEquals(v, 1024.1024);
	}

	@Test
	void test4() {
		FastByteBuffer buffer1 = new FastByteBuffer(32);
		buffer1.append(new byte[32]);
		buffer1.setIntBE(0, -1);
		buffer1.setShortBE(4, -1);
		buffer1.setMediumBE(6, -1);
		ByteBuffer byteBuffer = buffer1.toBuffer();
		long i = ByteBufferUtil.readUnsignedIntBE(byteBuffer);
		int s = ByteBufferUtil.readUnsignedShortBE(byteBuffer);
		int m = ByteBufferUtil.readUnsignedMediumBE(byteBuffer);
		Assertions.assertEquals(i, 4294967295L);
		Assertions.assertEquals(s, 65535);
		Assertions.assertEquals(m, 16777215);
	}

}
