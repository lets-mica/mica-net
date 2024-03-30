package org.tio.utils.hutool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tio.utils.buffer.ByteBufferUtil;

import java.nio.ByteBuffer;

/**
 * FastByteBuffer 测试
 *
 * @author L.cm
 */
public class FastByteBufferTest {

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

}
