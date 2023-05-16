package org.tio.utils.buffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

/**
 * ByteBufferUtil 单元测试
 */
class ByteBufferUtilTest {

	@Test
	void testShort() {
		int value = 65535;
		ByteBuffer buffer = ByteBuffer.allocate(2);
		ByteBufferUtil.writeUnsignedShort(buffer, value);
		buffer.flip();
		int unsignedShort = ByteBufferUtil.readUnsignedShort(buffer);
		Assertions.assertEquals(value, unsignedShort);
	}

	@Test
	void testShortBE() {
		int value = 65535;
		ByteBuffer buffer = ByteBuffer.allocate(2);
		ByteBufferUtil.writeUnsignedShortBE(buffer, value);
		buffer.flip();
		int unsignedShort = ByteBufferUtil.readUnsignedShortBE(buffer);
		Assertions.assertEquals(value, unsignedShort);
	}

	@Test
	void testInt() {
		long value = 4294967295L;
		ByteBuffer buffer = ByteBuffer.allocate(4);
		ByteBufferUtil.writeUnsignedInt(buffer, value);
		buffer.flip();
		long unsignedShort = ByteBufferUtil.readUnsignedInt(buffer);
		Assertions.assertEquals(value, unsignedShort);
	}

	@Test
	void testIntBE() {
		long value = 4294967295L;
		ByteBuffer buffer = ByteBuffer.allocate(4);
		ByteBufferUtil.writeUnsignedIntBE(buffer, value);
		buffer.flip();
		long unsignedShort = ByteBufferUtil.readUnsignedIntBE(buffer);
		Assertions.assertEquals(value, unsignedShort);
	}

}
