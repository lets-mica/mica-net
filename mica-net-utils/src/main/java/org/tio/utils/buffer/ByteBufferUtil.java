/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & dreamlu.net).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tio.utils.buffer;

import org.tio.utils.mica.HexUtils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * ByteBuffer 工具
 *
 * @author L.cm
 */
public class ByteBufferUtil {

	private ByteBufferUtil() {
	}

	/**
	 * 空 byte 数组
	 */
	public static final byte[] EMPTY_BYTES = new byte[0];

	/**
	 * read byte
	 *
	 * @param buffer ByteBuffer
	 * @return byte
	 */
	public static byte readByte(ByteBuffer buffer) {
		return buffer.get();
	}

	/**
	 * read unsigned byte，1个字节无符号
	 *
	 * @param buffer ByteBuffer
	 * @return short
	 */
	public static short readUnsignedByte(ByteBuffer buffer) {
		return (short) (buffer.get() & 0xFF);
	}

	/**
	 * 读取 byte 数组
	 *
	 * @param buffer ByteBuffer
	 * @param length 长度
	 * @return byte array
	 */
	public static byte[] readBytes(ByteBuffer buffer, int length) {
		byte[] data = new byte[length];
		buffer.get(data, 0, length);
		return data;
	}

	/**
	 * 读取 short
	 *
	 * @param buffer ByteBuffer
	 * @return short
	 */
	public static short readShort(ByteBuffer buffer) {
		return buffer.getShort();
	}

	/**
	 * 读取 short，小端
	 *
	 * @param buffer ByteBuffer
	 * @return short
	 */
	public static short readShortLE(ByteBuffer buffer) {
		byte[] value = new byte[2];
		buffer.get(value, 0, 2);
		short ret = value[0];
		ret |= (value[1] & 0xff) << 8;
		return ret;
	}

	/**
	 * 读取 short，大端
	 *
	 * @param buffer ByteBuffer
	 * @return short
	 */
	public static short readShortBE(ByteBuffer buffer) {
		byte[] value = new byte[2];
		buffer.get(value, 0, 2);
		short ret = (short) ((value[0]) << 8);
		ret |= value[1] & 0xff;
		return ret;
	}

	/**
	 * read unsigned short，2个字节无符号
	 *
	 * @param buffer ByteBuffer
	 * @return int
	 */
	public static int readUnsignedShortLE(ByteBuffer buffer) {
		byte[] value = new byte[2];
		buffer.get(value, 0, 2);
		int ret = value[0] & 0xff;
		ret |= (value[1] & 0xff) << 8;
		return ret;
	}

	/**
	 * read unsigned short，2个字节无符号，大端在前
	 *
	 * @param buffer ByteBuffer
	 * @return int
	 */
	public static int readUnsignedShortBE(ByteBuffer buffer) {
		byte[] value = new byte[2];
		buffer.get(value, 0, 2);
		int ret = (value[0] & 0xff) << 8;
		ret |= value[1] & 0xff;
		return ret;
	}

	/**
	 * read unsigned 3个字节无符号
	 *
	 * @param buffer ByteBuffer
	 * @return int
	 */
	public static int readUnsignedMediumLE(ByteBuffer buffer) {
		byte[] value = new byte[3];
		buffer.get(value, 0, 3);
		int ret = value[0] & 0xff;
		ret |= (value[1] & 0xff) << 8;
		ret |= (value[2] & 0xff) << 16;
		return ret;
	}

	/**
	 * read unsigned 3个字节无符号，大端在前
	 *
	 * @param buffer ByteBuffer
	 * @return int
	 */
	public static int readUnsignedMediumBE(ByteBuffer buffer) {
		byte[] value = new byte[3];
		buffer.get(value, 0, 3);
		int ret = (value[0] & 0xff) << 16;
		ret |= (value[1] & 0xff) << 8;
		ret |= value[2] & 0xff;
		return ret;
	}

	/**
	 * read int, 4个字节
	 *
	 * @param buffer ByteBuffer
	 * @return int
	 */
	public static int readInt(ByteBuffer buffer) {
		return buffer.getInt();
	}

	/**
	 * read int, 4个字节，小端
	 *
	 * @param buffer ByteBuffer
	 * @return int
	 */
	public static int readIntLE(ByteBuffer buffer) {
		byte[] value = new byte[4];
		buffer.get(value, 0, 4);
		int ret = value[0] & 0xff;
		ret |= (value[1] & 0xff) << 8;
		ret |= (value[2] & 0xff) << 16;
		ret |= value[3] << 24;
		return ret;
	}

	/**
	 * read int, 4个字节，大端在前
	 *
	 * @param buffer ByteBuffer
	 * @return int
	 */
	public static int readIntBE(ByteBuffer buffer) {
		byte[] value = new byte[4];
		buffer.get(value, 0, 4);
		int ret = value[0] << 24;
		ret |= (value[1] & 0xff) << 16;
		ret |= (value[2] & 0xff) << 8;
		ret |= value[3] & 0xff;
		return ret;
	}

	/**
	 * read unsigned int, 4个字节无符号
	 *
	 * @param buffer ByteBuffer
	 * @return long
	 */
	public static long readUnsignedIntLE(ByteBuffer buffer) {
		byte[] value = new byte[4];
		buffer.get(value, 0, 4);
		long ret = value[0] & 0xff;
		ret |= (value[1] & 0xff) << 8;
		ret |= (value[2] & 0xff) << 16;
		ret |= (long) (value[3] & 0xff) << 24;
		return ret;
	}

	/**
	 * read unsigned int, 4个字节无符号，大端在前
	 *
	 * @param buffer ByteBuffer
	 * @return long
	 */
	public static long readUnsignedIntBE(ByteBuffer buffer) {
		byte[] value = new byte[4];
		buffer.get(value, 0, 4);
		long ret = (long) (value[0] & 0xff) << 24;
		ret |= (value[1] & 0xff) << 16;
		ret |= (value[2] & 0xff) << 8;
		ret |= value[3] & 0xff;
		return ret;
	}

	/**
	 * read float, 4个字节，小端
	 *
	 * @param buffer ByteBuffer
	 * @return float
	 */
	public static float readFloat(ByteBuffer buffer) {
		return buffer.getFloat();
	}

	/**
	 * read float, 4个字节，小端
	 *
	 * @param buffer ByteBuffer
	 * @return float
	 */
	public static float readFloatLE(ByteBuffer buffer) {
		return Float.intBitsToFloat(readIntLE(buffer));
	}

	/**
	 * read float, 4个字节，大端
	 *
	 * @param buffer ByteBuffer
	 * @return float
	 */
	public static float readFloatBE(ByteBuffer buffer) {
		return Float.intBitsToFloat(readIntBE(buffer));
	}

	/**
	 * read long, 8个字节
	 *
	 * @param buffer ByteBuffer
	 * @return long
	 */
	public static long readLong(ByteBuffer buffer) {
		return buffer.getLong();
	}

	/**
	 * read long, 8个字节，无符号
	 *
	 * @param buffer ByteBuffer
	 * @return long
	 */
	public static long readLongLE(ByteBuffer buffer) {
		byte[] value = new byte[8];
		buffer.get(value, 0, 8);
		long ret = value[0] & 0xff;
		ret |= (value[1] & 0xff) << 8;
		ret |= (value[2] & 0xff) << 16;
		ret |= (long) (value[3] & 0xff) << 24;
		ret |= (long) (value[4] & 0xff) << 32;
		ret |= (long) (value[5] & 0xff) << 40;
		ret |= (long) (value[6] & 0xff) << 48;
		ret |= (long) value[7] << 56;
		return ret;
	}

	/**
	 * read long, 8个字节，无符号，大端在前
	 *
	 * @param buffer ByteBuffer
	 * @return long
	 */
	public static long readLongBE(ByteBuffer buffer) {
		byte[] value = new byte[8];
		buffer.get(value, 0, 8);
		long ret = (long) value[0] << 56;
		ret |= (long) (value[1] & 0xff) << 48;
		ret |= (long) (value[2] & 0xff) << 40;
		ret |= (long) (value[3] & 0xff) << 32;
		ret |= (long) (value[4] & 0xff) << 24;
		ret |= (value[5] & 0xff) << 16;
		ret |= (value[6] & 0xff) << 8;
		ret |= value[7] & 0xff;
		return ret;
	}

	/**
	 * read long, 8个字节，无符号
	 *
	 * @param buffer ByteBuffer
	 * @return long
	 */
	public static long readUnsignedLongLE(ByteBuffer buffer) {
		byte[] value = new byte[8];
		buffer.get(value, 0, 8);
		long ret = value[0] & 0xff;
		ret |= (value[1] & 0xff) << 8;
		ret |= (value[2] & 0xff) << 16;
		ret |= (long) (value[3] & 0xff) << 24;
		ret |= (long) (value[4] & 0xff) << 32;
		ret |= (long) (value[5] & 0xff) << 40;
		ret |= (long) (value[6] & 0xff) << 48;
		ret |= (long) (value[7] & 0xff) << 56;
		return ret;
	}

	/**
	 * read long, 8个字节，无符号，大端在前
	 *
	 * @param buffer ByteBuffer
	 * @return long
	 */
	public static long readUnsignedLongBE(ByteBuffer buffer) {
		byte[] value = new byte[8];
		buffer.get(value, 0, 8);
		long ret = (long) (value[0] & 0xff) << 56;
		ret |= (long) (value[1] & 0xff) << 48;
		ret |= (long) (value[2] & 0xff) << 40;
		ret |= (long) (value[3] & 0xff) << 32;
		ret |= (long) (value[4] & 0xff) << 24;
		ret |= (value[5] & 0xff) << 16;
		ret |= (value[6] & 0xff) << 8;
		ret |= value[7] & 0xff;
		return ret;
	}

	/**
	 * read double, 8个字节
	 *
	 * @param buffer ByteBuffer
	 * @return double
	 */
	public static double readDouble(ByteBuffer buffer) {
		return buffer.getDouble();
	}

	/**
	 * read double, 8个字节，小端在前
	 *
	 * @param buffer ByteBuffer
	 * @return double
	 */
	public static double readDoubleLE(ByteBuffer buffer) {
		return Double.longBitsToDouble(readLongLE(buffer));
	}

	/**
	 * read double, 8个字节，大端在前
	 *
	 * @param buffer ByteBuffer
	 * @return double
	 */
	public static double readDoubleBE(ByteBuffer buffer) {
		return Double.longBitsToDouble(readLongBE(buffer));
	}

	/**
	 * 写出 2 个字节的 short，小端模式
	 *
	 * @param buffer ByteBuffer
	 * @param s      数据
	 */
	public static void writeShortLE(ByteBuffer buffer, short s) {
		byte[] value = new byte[2];
		value[0] = (byte) s;
		value[1] = (byte) (s >> 8);
		buffer.put(value, 0, 2);
	}

	/**
	 * 写出 2 个字节的 short，大端模式
	 *
	 * @param buffer ByteBuffer
	 * @param s      数据
	 */
	public static void writeShortBE(ByteBuffer buffer, short s) {
		byte[] value = new byte[2];
		value[0] = (byte) (s >> 8);
		value[1] = (byte) s;
		buffer.put(value, 0, 2);
	}

	/**
	 * 写出 2 个字节的无符号 short
	 *
	 * @param buffer ByteBuffer
	 * @param i      数据
	 */
	public static void writeUnsignedShortLE(ByteBuffer buffer, int i) {
		byte[] value = new byte[2];
		value[0] = (byte) (i & 0xff);
		value[1] = (byte) (i >>> 8);
		buffer.put(value, 0, 2);
	}

	/**
	 * 写出 2 个字节的无符号 short，大端模式
	 *
	 * @param buffer ByteBuffer
	 * @param i      数据
	 */
	public static void writeUnsignedShortBE(ByteBuffer buffer, int i) {
		byte[] value = new byte[2];
		value[0] = (byte) (i >>> 8);
		value[1] = (byte) (i & 0xff);
		buffer.put(value, 0, 2);
	}

	/**
	 * 写出 3 个字节的无符号
	 *
	 * @param buffer ByteBuffer
	 * @param i      数据
	 */
	public static void writeUnsignedMediumLE(ByteBuffer buffer, int i) {
		byte[] value = new byte[3];
		value[0] = (byte) (i & 0xff);
		value[1] = (byte) (i >>> 8);
		value[2] = (byte) (i >>> 16);
		buffer.put(value, 0, 3);
	}

	/**
	 * 写出 3 个字节的无符号，大端模式
	 *
	 * @param buffer ByteBuffer
	 * @param i      数据
	 */
	public static void writeUnsignedMediumBE(ByteBuffer buffer, int i) {
		byte[] value = new byte[3];
		value[0] = (byte) (i >>> 16);
		value[1] = (byte) (i >>> 8);
		value[2] = (byte) (i & 0xff);
		buffer.put(value, 0, 3);
	}

	/**
	 * 写出 4 个字节的 int，小端模式
	 *
	 * @param buffer ByteBuffer
	 * @param i      数据
	 */
	public static void writeIntLE(ByteBuffer buffer, int i) {
		byte[] value = new byte[4];
		value[0] = (byte) i;
		value[1] = (byte) (i >> 8);
		value[2] = (byte) (i >> 16);
		value[3] = (byte) (i >> 24);
		buffer.put(value, 0, 4);
	}

	/**
	 * 写出 4 个字节的 int，大端模式
	 *
	 * @param buffer ByteBuffer
	 * @param i      数据
	 */
	public static void writeIntBE(ByteBuffer buffer, int i) {
		byte[] value = new byte[4];
		value[0] = (byte) (i >> 24);
		value[1] = (byte) (i >> 16);
		value[2] = (byte) (i >> 8);
		value[3] = (byte) i;
		buffer.put(value, 0, 4);
	}

	/**
	 * 写出 4 个字节的无符号 int
	 *
	 * @param buffer ByteBuffer
	 * @param l      数据
	 */
	public static void writeUnsignedIntLE(ByteBuffer buffer, long l) {
		byte[] value = new byte[4];
		value[0] = (byte) (l & 0xff);
		value[1] = (byte) (l >>> 8);
		value[2] = (byte) (l >>> 16);
		value[3] = (byte) (l >>> 24);
		buffer.put(value, 0, 4);
	}

	/**
	 * 写出 4 个字节的无符号 int，大端模式
	 *
	 * @param buffer ByteBuffer
	 * @param l      数据
	 */
	public static void writeUnsignedIntBE(ByteBuffer buffer, long l) {
		byte[] value = new byte[4];
		value[0] = (byte) (l >>> 24);
		value[1] = (byte) (l >>> 16);
		value[2] = (byte) (l >>> 8);
		value[3] = (byte) (l & 0xff);
		buffer.put(value, 0, 4);
	}

	/**
	 * 写出 4 个字节的 float
	 *
	 * @param buffer ByteBuffer
	 * @param value  数据
	 */
	public static void writeFloat(ByteBuffer buffer, float value) {
		buffer.putFloat(value);
	}

	/**
	 * 写出 4 个字节的 float，小端模式
	 *
	 * @param buffer ByteBuffer
	 * @param value  数据
	 */
	public static void writeFloatLE(ByteBuffer buffer, float value) {
		writeIntLE(buffer, Float.floatToRawIntBits(value));
	}

	/**
	 * 写出 4 个字节的 float，大端模式
	 *
	 * @param buffer ByteBuffer
	 * @param value  数据
	 */
	public static void writeFloatBE(ByteBuffer buffer, float value) {
		writeIntBE(buffer, Float.floatToRawIntBits(value));
	}

	/**
	 * 写出 8 个字节的 long，小端模式
	 *
	 * @param buffer ByteBuffer
	 * @param l      数据
	 */
	public static void writeLongLE(ByteBuffer buffer, long l) {
		byte[] value = new byte[8];
		value[0] = (byte) l;
		value[1] = (byte) (l >> 8);
		value[2] = (byte) (l >> 16);
		value[3] = (byte) (l >> 24);
		value[4] = (byte) (l >> 32);
		value[5] = (byte) (l >> 40);
		value[6] = (byte) (l >> 48);
		value[7] = (byte) (l >> 56);
		buffer.put(value, 0, 8);
	}

	/**
	 * 写出 8 个字节的 long，大端模式
	 *
	 * @param buffer ByteBuffer
	 * @param l      数据
	 */
	public static void writeLongBE(ByteBuffer buffer, long l) {
		byte[] value = new byte[8];
		value[0] = (byte) (l >> 56);
		value[1] = (byte) (l >> 48);
		value[2] = (byte) (l >> 40);
		value[3] = (byte) (l >> 32);
		value[4] = (byte) (l >> 24);
		value[5] = (byte) (l >> 16);
		value[6] = (byte) (l >> 8);
		value[7] = (byte) l;
		buffer.put(value, 0, 8);
	}

	/**
	 * 写出 8 个字节的无符号 long
	 *
	 * @param buffer ByteBuffer
	 * @param l      数据
	 */
	public static void writeUnsignedLongLE(ByteBuffer buffer, long l) {
		byte[] value = new byte[8];
		value[0] = (byte) (l & 0xff);
		value[1] = (byte) (l >>> 8);
		value[2] = (byte) (l >>> 16);
		value[3] = (byte) (l >>> 24);
		value[4] = (byte) (l >>> 32);
		value[5] = (byte) (l >>> 40);
		value[6] = (byte) (l >>> 48);
		value[7] = (byte) (l >>> 56);
		buffer.put(value, 0, 8);
	}

	/**
	 * 写出 8 个字节的无符号 long，大端模式
	 *
	 * @param buffer ByteBuffer
	 * @param l      数据
	 */
	public static void writeUnsignedLongBE(ByteBuffer buffer, long l) {
		byte[] value = new byte[8];
		value[0] = (byte) (l >>> 56);
		value[1] = (byte) (l >>> 48);
		value[2] = (byte) (l >>> 40);
		value[3] = (byte) (l >>> 32);
		value[4] = (byte) (l >>> 24);
		value[5] = (byte) (l >>> 16);
		value[6] = (byte) (l >>> 8);
		value[7] = (byte) (l & 0xff);
		buffer.put(value, 0, 8);
	}

	/**
	 * 写出 8 个字节的 value
	 *
	 * @param buffer ByteBuffer
	 * @param value  数据
	 */
	public static void writeDouble(ByteBuffer buffer, double value) {
		buffer.putDouble(value);
	}

	/**
	 * 写出 8 个字节的 value，小端模式
	 *
	 * @param buffer ByteBuffer
	 * @param value  数据
	 */
	public static void writeDoubleLE(ByteBuffer buffer, double value) {
		writeLongLE(buffer, Double.doubleToRawLongBits(value));
	}

	/**
	 * 写出 8 个字节的 value，大端模式
	 *
	 * @param buffer ByteBuffer
	 * @param value  数据
	 */
	public static void writeDoubleBE(ByteBuffer buffer, double value) {
		writeLongBE(buffer, Double.doubleToRawLongBits(value));
	}

	/**
	 * skip bytes
	 *
	 * @param buffer ByteBuffer
	 * @param skip   skip bytes
	 * @return ByteBuffer
	 */
	public static ByteBuffer skipBytes(ByteBuffer buffer, int skip) {
		buffer.position(buffer.position() + skip);
		return buffer;
	}

	/**
	 * 组合两个 bytebuffer，把可读部分的组合成一个新的 bytebuffer
	 *
	 * @param byteBuffer1 ByteBuffer
	 * @param byteBuffer2 ByteBuffer
	 * @return ByteBuffer
	 */
	public static ByteBuffer composite(ByteBuffer byteBuffer1, ByteBuffer byteBuffer2) {
		int capacity = byteBuffer1.remaining() + byteBuffer2.remaining();
		ByteBuffer ret = ByteBuffer.allocate(capacity);
		ret.put(byteBuffer1);
		ret.put(byteBuffer2);
		ret.position(0);
		ret.limit(ret.capacity());
		return ret;
	}

	/**
	 * ByteBuffer clone
	 *
	 * @param original ByteBuffer
	 * @return ByteBuffer
	 */
	public static ByteBuffer clone(ByteBuffer original) {
		ByteBuffer clone = ByteBuffer.allocate(original.capacity());
		// copy from the beginning
		original.rewind();
		clone.put(original);
		original.rewind();
		clone.flip();
		return clone;
	}

	/**
	 * @param src            ByteBuffer
	 * @param srcStartIndex  srcStartIndex
	 * @param dest           ByteBuffer
	 * @param destStartIndex destStartIndex
	 * @param length         length
	 */
	public static void copy(ByteBuffer src, int srcStartIndex, ByteBuffer dest, int destStartIndex, int length) {
		System.arraycopy(src.array(), srcStartIndex, dest.array(), destStartIndex, length);
	}

	/**
	 * @param src        本方法不会改变position等指针变量
	 * @param startIndex 从0开始
	 * @param endIndex   endIndex
	 * @return ByteBuffer
	 */
	public static ByteBuffer copy(ByteBuffer src, int startIndex, int endIndex) {
		int size = endIndex - startIndex;
		int initPosition = src.position();
		int initLimit = src.limit();

		src.position(startIndex);
		src.limit(endIndex);
		ByteBuffer ret = ByteBuffer.allocate(size);
		ret.put(src);
		ret.flip();

		src.position(initPosition);
		src.limit(initLimit);
		return ret;
	}

	/**
	 * @param src 本方法不会改变position等指针变量
	 * @return ByteBuffer
	 */
	public static ByteBuffer copy(ByteBuffer src) {
		int startIndex = src.position();
		int endIndex = src.limit();
		return copy(src, startIndex, endIndex);
	}

	/**
	 * @param src      ByteBuffer
	 * @param unitSize 每个单元的大小
	 * @return 如果不需要拆分，则返回null
	 */
	public static ByteBuffer[] split(ByteBuffer src, int unitSize) {
		int limit = src.limit();
		if (unitSize >= limit) {
			return null;
		}
		int size = (int) (Math.ceil((double) src.limit() / (double) unitSize));
		ByteBuffer[] ret = new ByteBuffer[size];
		int srcIndex = 0;
		for (int i = 0; i < size; i++) {
			int bufferSize = unitSize;
			if (i == size - 1) {
				bufferSize = src.limit() % unitSize;
			}
			byte[] dest = new byte[bufferSize];
			System.arraycopy(src.array(), srcIndex, dest, 0, dest.length);
			srcIndex = srcIndex + bufferSize;

			ret[i] = ByteBuffer.wrap(dest);
			ret[i].position(0);
			ret[i].limit(ret[i].capacity());
		}
		return ret;
	}

	/**
	 * @param buffer ByteBuffer
	 * @return index
	 */
	public static int lineEnd(ByteBuffer buffer) {
		return lineEnd(buffer, Integer.MAX_VALUE);
	}

	/**
	 * @param buffer    ByteBuffer
	 * @param maxLength maxLength
	 * @return index
	 */
	public static int lineEnd(ByteBuffer buffer, int maxLength) {
		int initPosition = buffer.position();
		int endPosition = indexOf(buffer, '\n', maxLength);
		if ((endPosition - initPosition > 0) && (buffer.get(endPosition - 1) == '\r')) {
			return endPosition - 1;
		}
		return endPosition;
	}

	/**
	 * @param buffer    position会被移动
	 * @param theChar   结束
	 * @param maxLength maxLength
	 * @return index
	 */
	public static int indexOf(ByteBuffer buffer, char theChar, int maxLength) {
		int count = 0;
		boolean needJudgeLengthOverflow = buffer.remaining() > maxLength;
		while (buffer.hasRemaining()) {
			if (buffer.get() == theChar) {
				return buffer.position() - 1;
			}
			if (needJudgeLengthOverflow) {
				count++;
				if (count > maxLength) {
					throw new IndexOutOfBoundsException("maxlength is " + maxLength);
				}
			}
		}
		return -1;
	}

	/**
	 * 读取一行
	 *
	 * @param buffer  ByteBuffer
	 * @param charset Charset
	 * @return String
	 */
	public static String readLine(ByteBuffer buffer, Charset charset) {
		return readLine(buffer, charset, Integer.MAX_VALUE);
	}

	/**
	 * @param buffer    ByteBuffer
	 * @param charset   Charset
	 * @param maxLength maxLength
	 * @return String
	 */
	public static String readLine(ByteBuffer buffer, Charset charset, int maxLength) {
		int startPosition = buffer.position();
		int endPosition = lineEnd(buffer, maxLength);
		if (endPosition == -1) {
			return null;
		}
		int nowPosition = buffer.position();
		if (endPosition > startPosition) {
			byte[] bs = new byte[endPosition - startPosition];
			buffer.position(startPosition);
			buffer.get(bs);
			buffer.position(nowPosition);
			if (charset == null) {
				return new String(bs);
			} else {
				return new String(bs, charset);
			}
		} else if (endPosition == startPosition) {
			return "";
		}
		return null;
	}

	/**
	 * 读取字符串
	 *
	 * @param buffer ByteBuffer
	 * @return String
	 */
	public static String readString(ByteBuffer buffer, int count) {
		return readString(buffer, count, StandardCharsets.UTF_8);
	}

	/**
	 * 读取字符串
	 *
	 * @param buffer ByteBuffer
	 * @return String
	 */
	public static String readString(ByteBuffer buffer, int count, Charset charset) {
		byte[] bytes = new byte[count];
		buffer.get(bytes);
		return new String(bytes, charset);
	}

	/**
	 * @param buffer    ByteBuffer
	 * @param charset   Charset
	 * @param endChar   endChar
	 * @param maxLength maxLength
	 * @return String
	 */
	public static String readString(ByteBuffer buffer, Charset charset, char endChar, int maxLength) {
		int startPosition = buffer.position();
		int endPosition = indexOf(buffer, endChar, maxLength);
		if (endPosition == -1) {
			return null;
		}
		int nowPosition = buffer.position();
		if (endPosition > startPosition) {
			byte[] bs = new byte[endPosition - startPosition];
			buffer.position(startPosition);
			buffer.get(bs);
			buffer.position(nowPosition);
			if (charset == null) {
				return new String(bs);
			} else {
				return new String(bs, charset);
			}
		} else if (endPosition == startPosition) {
			return "";
		}
		return null;
	}

	/**
	 * 转成 string，读取 ByteBuffer 所有数据
	 *
	 * @param buffer ByteBuffer
	 * @return 字符串
	 */
	public static String toString(ByteBuffer buffer) {
		return toString(buffer, StandardCharsets.UTF_8);
	}

	/**
	 * 转成 string，读取 ByteBuffer 所有数据
	 *
	 * @param buffer  ByteBuffer
	 * @param charset Charset
	 * @return 字符串
	 */
	public static String toString(ByteBuffer buffer, Charset charset) {
		return new String(buffer.array(), charset);
	}

	/**
	 * 转成 string
	 *
	 * @param buffer ByteBuffer
	 * @return 字符串
	 */
	public static String toString(byte[] buffer) {
		return toString(buffer, StandardCharsets.UTF_8);
	}

	/**
	 * 转成 string
	 *
	 * @param buffer  ByteBuffer
	 * @param charset Charset
	 * @return 字符串
	 */
	public static String toString(byte[] buffer, Charset charset) {
		return new String(buffer, charset);
	}

	/**
	 * 以16进制 打印 ByteBuffer
	 *
	 * @param byteBuffer ByteBuffer
	 */
	public static String hexDump(ByteBuffer byteBuffer) {
		byte[] data = Arrays.copyOf(byteBuffer.array(), byteBuffer.remaining());
		return toHexString(data);
	}

	/**
	 * 以16进制 打印字节数组
	 *
	 * @param bytes byte[]
	 */
	public static String toHexString(final byte[] bytes) {
		final StringBuilder buffer = new StringBuilder(bytes.length);
		buffer.append("\r\n\t\t   0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f\r\n");
		int startIndex = 0;
		int column = 0;
		for (int i = 0; i < bytes.length; i++) {
			column = i % 16;
			switch (column) {
				case 0:
					startIndex = i;
					buffer.append(fixHexString(Integer.toHexString(i))).append(": ");
					buffer.append(HexUtils.encode(bytes[i]));
					buffer.append(' ');
					break;
				case 15:
					buffer.append(HexUtils.encode(bytes[i]));
					buffer.append(" ; ");
					buffer.append(filterString(bytes, startIndex, column + 1));
					buffer.append("\r\n");
					break;
				default:
					buffer.append(HexUtils.encode(bytes[i]));
					buffer.append(' ');
			}
		}
		if (column != 15) {
			for (int i = 0; i < 15 - column; i++) {
				buffer.append("   ");
			}
			buffer.append("; ").append(filterString(bytes, startIndex, column + 1));
			buffer.append("\r\n");
		}

		return buffer.toString();
	}

	/**
	 * 过滤掉字节数组中0x0 - 0x1F的控制字符，生成字符串
	 *
	 * @param bytes  byte[]
	 * @param offset int
	 * @param count  int
	 * @return String
	 */
	private static String filterString(final byte[] bytes, final int offset, final int count) {
		final byte[] buffer = new byte[count];
		System.arraycopy(bytes, offset, buffer, 0, count);
		for (int i = 0; i < count; i++) {
			if (buffer[i] >= 0x0 && buffer[i] <= 0x1F) {
				buffer[i] = 0x2e;
			}
		}
		return new String(buffer);
	}

	/**
	 * 将hexStr格式化成length长度16进制数，并在后边加上h
	 *
	 * @param hexStr String
	 * @return String
	 */
	private static String fixHexString(final String hexStr) {
		if (hexStr == null || hexStr.length() == 0) {
			return "00000000h";
		} else {
			final StringBuilder buf = new StringBuilder(8);
			final int strLen = hexStr.length();
			for (int i = 0; i < 8 - strLen; i++) {
				buf.append('0');
			}
			buf.append(hexStr).append('h');
			return buf.toString();
		}
	}

}
