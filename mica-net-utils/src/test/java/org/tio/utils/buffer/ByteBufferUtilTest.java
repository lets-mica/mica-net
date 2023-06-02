package org.tio.utils.buffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

/**
 * ByteBufferUtil 单元测试
 */
class ByteBufferUtilTest {

	@Test
	void test() {
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
	void testShort() {
		int value = 65535;
		ByteBuffer buffer = ByteBuffer.allocate(6);
		ByteBufferUtil.writeUnsignedShort(buffer, value);
		ByteBufferUtil.writeIntBE(buffer, value);
		buffer.flip();
		int unsignedShort = ByteBufferUtil.readUnsignedShort(buffer);
		Assertions.assertEquals(value, unsignedShort);
		int shortValue = ByteBufferUtil.readIntBE(buffer);
		Assertions.assertEquals(value, shortValue);
	}

	@Test
	void testShortBE() {
		int value = 65535;
		ByteBuffer buffer = ByteBuffer.allocate(6);
		ByteBufferUtil.writeUnsignedShortBE(buffer, value);
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
		ByteBufferUtil.writeUnsignedMedium(buffer, value);
		buffer.flip();
		int unsignedShort = ByteBufferUtil.readUnsignedMedium(buffer);
		Assertions.assertEquals(value, unsignedShort);
	}

	@Test
	void testMediumBE() {
		int value = 16777215;
		ByteBuffer buffer = ByteBuffer.allocate(3);
		ByteBufferUtil.writeUnsignedMediumBE(buffer, value);
		buffer.flip();
		int unsignedShort = ByteBufferUtil.readUnsignedMediumBE(buffer);
		Assertions.assertEquals(value, unsignedShort);
	}

	@Test
	void testInt() {
		long value = 4294967295L;
		ByteBuffer buffer = ByteBuffer.allocate(12);
		ByteBufferUtil.writeUnsignedInt(buffer, value);
		ByteBufferUtil.writeLongBE(buffer, value);
		buffer.flip();
		long unsignedShort = ByteBufferUtil.readUnsignedInt(buffer);
		Assertions.assertEquals(value, unsignedShort);
		long intValue = ByteBufferUtil.readLongBE(buffer);
		Assertions.assertEquals(value, intValue);
	}

	@Test
	void testIntBE() {
		long value = 4294967295L;
		ByteBuffer buffer = ByteBuffer.allocate(12);
		ByteBufferUtil.writeUnsignedIntBE(buffer, value);
		ByteBufferUtil.writeLongLE(buffer, value);
		buffer.flip();
		long unsignedShort = ByteBufferUtil.readUnsignedIntBE(buffer);
		Assertions.assertEquals(value, unsignedShort);
		long intValue = ByteBufferUtil.readLongLE(buffer);
		Assertions.assertEquals(value, intValue);
	}

}
