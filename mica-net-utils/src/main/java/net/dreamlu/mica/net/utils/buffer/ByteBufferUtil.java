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

package net.dreamlu.mica.net.utils.buffer;

import net.dreamlu.mica.net.utils.mica.HexUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * ByteBuffer 工具类，提供ByteBuffer的读写、复制、转换等操作
 * <p>
 * 支持堆缓冲区和直接缓冲区，自动选择最优实现方式。
 * LE后缀表示小端序（Little Endian），BE后缀表示大端序（Big Endian）。
 *
 * @author L.cm
 */
public class ByteBufferUtil {

	/**
	 * 空 byte 数组
	 */
	public static final byte[] EMPTY_BYTES = new byte[0];

	private ByteBufferUtil() {
	}

	/**
	 * 读取1个字节
	 *
	 * @param buffer ByteBuffer
	 * @return byte值
	 */
	public static byte readByte(ByteBuffer buffer) {
		return buffer.get();
	}

	/**
	 * 读取1个字节作为无符号数
	 *
	 * @param buffer ByteBuffer
	 * @return short类型的无符号值 (0-255)
	 */
	public static short readUnsignedByte(ByteBuffer buffer) {
		return (short) (buffer.get() & 0xFF);
	}

	/**
	 * 读取指定长度的字节数组，消耗position
	 *
	 * @param buffer ByteBuffer
	 * @param length 要读取的字节数
	 * @return byte数组
	 */
	public static byte[] readBytes(ByteBuffer buffer, int length) {
		byte[] data = new byte[length];
		buffer.get(data, 0, length);
		return data;
	}

	/**
	 * 读取ByteBuffer remaining区间的字节数组，不消耗position
	 * <p>
	 * 自动处理堆缓冲区和直接缓冲区。
	 *
	 * @param buffer ByteBuffer
	 * @return remaining区间的字节数组
	 */
	public static byte[] toArray(ByteBuffer buffer) {
		int position = buffer.position();
		int limit = buffer.limit();
		int remaining = limit - position;
		if (buffer.hasArray()) {
			// 堆缓冲区：直接复制指定区间
			return Arrays.copyOfRange(buffer.array(), buffer.arrayOffset() + position, buffer.arrayOffset() + limit);
		} else {
			// 直接缓冲区：读取后恢复position
			byte[] data = new byte[remaining];
			buffer.get(data);
			buffer.position(position);
			return data;
		}
	}

	/**
	 * 读取2字节short，使用buffer当前字节序
	 *
	 * @param buffer ByteBuffer
	 * @return short值
	 */
	public static short readShort(ByteBuffer buffer) {
		return buffer.getShort();
	}

	/**
	 * 读取2字节short，小端序
	 *
	 * @param buffer ByteBuffer
	 * @return short值
	 */
	public static short readShortLE(ByteBuffer buffer) {
		return (short) ((buffer.get() & 0xFF) | ((buffer.get() & 0xFF) << 8));
	}

	/**
	 * 读取2字节short，大端序
	 *
	 * @param buffer ByteBuffer
	 * @return short值
	 */
	public static short readShortBE(ByteBuffer buffer) {
		return (short) (((buffer.get() & 0xFF) << 8) | (buffer.get() & 0xFF));
	}

	/**
	 * 读取2字节无符号整数，使用buffer当前字节序
	 *
	 * @param buffer ByteBuffer
	 * @return int类型的无符号值 (0-65535)
	 */
	public static int readUnsignedShort(ByteBuffer buffer) {
		ByteOrder order = buffer.order();
		if (ByteOrder.BIG_ENDIAN == order) {
			return readUnsignedShortBE(buffer);
		} else {
			return readUnsignedShortLE(buffer);
		}
	}

	/**
	 * 读取2字节无符号整数，小端序
	 *
	 * @param buffer ByteBuffer
	 * @return int类型的无符号值
	 */
	public static int readUnsignedShortLE(ByteBuffer buffer) {
		return (buffer.get() & 0xFF) | ((buffer.get() & 0xFF) << 8);
	}

	/**
	 * 读取2字节无符号整数，大端序
	 *
	 * @param buffer ByteBuffer
	 * @return int类型的无符号值
	 */
	public static int readUnsignedShortBE(ByteBuffer buffer) {
		return ((buffer.get() & 0xFF) << 8) | (buffer.get() & 0xFF);
	}

	/**
	 * 读取3字节有符号整数，使用buffer当前字节序
	 * <p>
	 * 3字节整数常用于网络协议和音视频格式中。
	 *
	 * @param buffer ByteBuffer
	 * @return int值
	 */
	public static int readMedium(ByteBuffer buffer) {
		ByteOrder order = buffer.order();
		if (ByteOrder.BIG_ENDIAN == order) {
			return readMediumBE(buffer);
		} else {
			return readMediumLE(buffer);
		}
	}

	/**
	 * 读取3字节有符号整数，小端序
	 *
	 * @param buffer ByteBuffer
	 * @return int值
	 */
	public static int readMediumLE(ByteBuffer buffer) {
		int ret = readUnsignedMediumLE(buffer);
		// 如果最高位为1，则表示是负数，需要符号扩展
		if ((ret & 0x800000) != 0) {
			ret |= 0xff000000;
		}
		return ret;
	}

	/**
	 * 读取3字节有符号整数，大端序
	 *
	 * @param buffer ByteBuffer
	 * @return int值
	 */
	public static int readMediumBE(ByteBuffer buffer) {
		int ret = readUnsignedMediumBE(buffer);
		// 如果最高位为1，则表示是负数，需要符号扩展
		if ((ret & 0x800000) != 0) {
			ret |= 0xff000000;
		}
		return ret;
	}

	/**
	 * 读取3字节无符号整数，使用buffer当前字节序
	 *
	 * @param buffer ByteBuffer
	 * @return int类型的无符号值 (0-16777215)
	 */
	public static int readUnsignedMedium(ByteBuffer buffer) {
		ByteOrder order = buffer.order();
		if (ByteOrder.BIG_ENDIAN == order) {
			return readUnsignedMediumBE(buffer);
		} else {
			return readUnsignedMediumLE(buffer);
		}
	}

	/**
	 * 读取3字节无符号整数，小端序
	 *
	 * @param buffer ByteBuffer
	 * @return int类型的无符号值
	 */
	public static int readUnsignedMediumLE(ByteBuffer buffer) {
		return (buffer.get() & 0xFF) | ((buffer.get() & 0xFF) << 8) | ((buffer.get() & 0xFF) << 16);
	}

	/**
	 * 读取3字节无符号整数，大端序
	 *
	 * @param buffer ByteBuffer
	 * @return int类型的无符号值
	 */
	public static int readUnsignedMediumBE(ByteBuffer buffer) {
		return ((buffer.get() & 0xFF) << 16) | ((buffer.get() & 0xFF) << 8) | (buffer.get() & 0xFF);
	}

	/**
	 * 读取4字节int，使用buffer当前字节序
	 *
	 * @param buffer ByteBuffer
	 * @return int值
	 */
	public static int readInt(ByteBuffer buffer) {
		return buffer.getInt();
	}

	/**
	 * 读取4字节int，小端序
	 *
	 * @param buffer ByteBuffer
	 * @return int值
	 */
	public static int readIntLE(ByteBuffer buffer) {
		return (buffer.get() & 0xFF) |
			((buffer.get() & 0xFF) << 8) |
			((buffer.get() & 0xFF) << 16) |
			((buffer.get() & 0xFF) << 24);
	}

	/**
	 * 读取4字节int，大端序
	 *
	 * @param buffer ByteBuffer
	 * @return int值
	 */
	public static int readIntBE(ByteBuffer buffer) {
		return ((buffer.get() & 0xFF) << 24) |
			((buffer.get() & 0xFF) << 16) |
			((buffer.get() & 0xFF) << 8) |
			(buffer.get() & 0xFF);
	}

	/**
	 * 读取4字节无符号整数，使用buffer当前字节序
	 *
	 * @param buffer ByteBuffer
	 * @return long类型的无符号值
	 */
	public static long readUnsignedInt(ByteBuffer buffer) {
		ByteOrder order = buffer.order();
		if (ByteOrder.BIG_ENDIAN == order) {
			return readUnsignedIntBE(buffer);
		} else {
			return readUnsignedIntLE(buffer);
		}
	}

	/**
	 * 读取4字节无符号整数，小端序
	 *
	 * @param buffer ByteBuffer
	 * @return long类型的无符号值
	 */
	public static long readUnsignedIntLE(ByteBuffer buffer) {
		return (buffer.get() & 0xFFL) |
			((buffer.get() & 0xFFL) << 8) |
			((buffer.get() & 0xFFL) << 16) |
			((buffer.get() & 0xFFL) << 24);
	}

	/**
	 * 读取4字节无符号整数，大端序
	 *
	 * @param buffer ByteBuffer
	 * @return long类型的无符号值
	 */
	public static long readUnsignedIntBE(ByteBuffer buffer) {
		return ((buffer.get() & 0xFFL) << 24) |
			((buffer.get() & 0xFFL) << 16) |
			((buffer.get() & 0xFFL) << 8) |
			(buffer.get() & 0xFFL);
	}

	/**
	 * 读取n字节无符号整数，大端序
	 *
	 * @param buffer ByteBuffer
	 * @param n      字节数 (1-8)
	 * @return long类型的无符号值
	 */
	public static long readUnsignedNByteBE(ByteBuffer buffer, int n) {
		byte[] value = new byte[n];
		buffer.get(value, 0, n);
		long ret = 0;
		for (int i = 0; i < n; i++) {
			ret |= (long) (value[i] & 0xff) << (8 * (n - i - 1));
		}
		return ret;
	}

	/**
	 * 读取n字节无符号整数，小端序
	 *
	 * @param buffer ByteBuffer
	 * @param n      字节数 (1-8)
	 * @return long类型的无符号值
	 */
	public static long readUnsignedNByteLE(ByteBuffer buffer, int n) {
		byte[] value = new byte[n];
		buffer.get(value, 0, n);
		long ret = 0;
		for (int i = 0; i < n; i++) {
			ret |= (long) (value[i] & 0xff) << (8 * i);
		}
		return ret;
	}

	/**
	 * 读取4字节float，使用buffer当前字节序
	 *
	 * @param buffer ByteBuffer
	 * @return float值
	 */
	public static float readFloat(ByteBuffer buffer) {
		return buffer.getFloat();
	}

	/**
	 * 读取4字节float，小端序
	 *
	 * @param buffer ByteBuffer
	 * @return float值
	 */
	public static float readFloatLE(ByteBuffer buffer) {
		return Float.intBitsToFloat(readIntLE(buffer));
	}

	/**
	 * 读取4字节float，大端序
	 *
	 * @param buffer ByteBuffer
	 * @return float值
	 */
	public static float readFloatBE(ByteBuffer buffer) {
		return Float.intBitsToFloat(readIntBE(buffer));
	}

	/**
	 * 读取8字节long，使用buffer当前字节序
	 *
	 * @param buffer ByteBuffer
	 * @return long值
	 */
	public static long readLong(ByteBuffer buffer) {
		return buffer.getLong();
	}

	/**
	 * 读取8字节long，小端序
	 *
	 * @param buffer ByteBuffer
	 * @return long值
	 */
	public static long readLongLE(ByteBuffer buffer) {
		return (buffer.get() & 0xFFL) |
			((buffer.get() & 0xFFL) << 8) |
			((buffer.get() & 0xFFL) << 16) |
			((buffer.get() & 0xFFL) << 24) |
			((buffer.get() & 0xFFL) << 32) |
			((buffer.get() & 0xFFL) << 40) |
			((buffer.get() & 0xFFL) << 48) |
			((long) buffer.get() << 56);
	}

	/**
	 * 读取8字节long，大端序
	 *
	 * @param buffer ByteBuffer
	 * @return long值
	 */
	public static long readLongBE(ByteBuffer buffer) {
		return ((long) buffer.get() << 56) |
			((buffer.get() & 0xFFL) << 48) |
			((buffer.get() & 0xFFL) << 40) |
			((buffer.get() & 0xFFL) << 32) |
			((buffer.get() & 0xFFL) << 24) |
			((buffer.get() & 0xFFL) << 16) |
			((buffer.get() & 0xFFL) << 8) |
			(buffer.get() & 0xFFL);
	}

	/**
	 * 读取8字节无符号整数，使用buffer当前字节序
	 *
	 * @param buffer ByteBuffer
	 * @return long值
	 */
	public static long readUnsignedLong(ByteBuffer buffer) {
		ByteOrder order = buffer.order();
		if (ByteOrder.BIG_ENDIAN == order) {
			return readUnsignedLongBE(buffer);
		} else {
			return readUnsignedLongLE(buffer);
		}
	}

	/**
	 * 读取8字节无符号整数，小端序
	 *
	 * @param buffer ByteBuffer
	 * @return long值
	 */
	public static long readUnsignedLongLE(ByteBuffer buffer) {
		return (buffer.get() & 0xFFL) |
			((buffer.get() & 0xFFL) << 8) |
			((buffer.get() & 0xFFL) << 16) |
			((buffer.get() & 0xFFL) << 24) |
			((buffer.get() & 0xFFL) << 32) |
			((buffer.get() & 0xFFL) << 40) |
			((buffer.get() & 0xFFL) << 48) |
			((buffer.get() & 0xFFL) << 56);
	}

	/**
	 * 读取8字节无符号整数，大端序
	 *
	 * @param buffer ByteBuffer
	 * @return long值
	 */
	public static long readUnsignedLongBE(ByteBuffer buffer) {
		return ((buffer.get() & 0xFFL) << 56) |
			((buffer.get() & 0xFFL) << 48) |
			((buffer.get() & 0xFFL) << 40) |
			((buffer.get() & 0xFFL) << 32) |
			((buffer.get() & 0xFFL) << 24) |
			((buffer.get() & 0xFFL) << 16) |
			((buffer.get() & 0xFFL) << 8) |
			(buffer.get() & 0xFFL);
	}

	/**
	 * 读取8字节double，使用buffer当前字节序
	 *
	 * @param buffer ByteBuffer
	 * @return double值
	 */
	public static double readDouble(ByteBuffer buffer) {
		return buffer.getDouble();
	}

	/**
	 * 读取8字节double，小端序
	 *
	 * @param buffer ByteBuffer
	 * @return double值
	 */
	public static double readDoubleLE(ByteBuffer buffer) {
		return Double.longBitsToDouble(readLongLE(buffer));
	}

	/**
	 * 读取8字节double，大端序
	 *
	 * @param buffer ByteBuffer
	 * @return double值
	 */
	public static double readDoubleBE(ByteBuffer buffer) {
		return Double.longBitsToDouble(readLongBE(buffer));
	}

	/**
	 * 写入1个字节
	 *
	 * @param buffer ByteBuffer
	 * @param s      数据 (0-255)
	 */
	public static void writeByte(ByteBuffer buffer, short s) {
		buffer.put((byte) s);
	}

	/**
	 * 写入2字节short，小端序
	 *
	 * @param buffer ByteBuffer
	 * @param s      数据
	 */
	public static void writeShortLE(ByteBuffer buffer, int s) {
		buffer.put((byte) s);
		buffer.put((byte) (s >> 8));
	}

	/**
	 * 写入2字节short，大端序
	 *
	 * @param buffer ByteBuffer
	 * @param s      数据
	 */
	public static void writeShortBE(ByteBuffer buffer, int s) {
		buffer.put((byte) (s >> 8));
		buffer.put((byte) s);
	}

	/**
	 * 写入3字节无符号整数，小端序
	 *
	 * @param buffer ByteBuffer
	 * @param i      数据 (0-16777215)
	 */
	public static void writeMediumLE(ByteBuffer buffer, int i) {
		buffer.put((byte) i);
		buffer.put((byte) (i >> 8));
		buffer.put((byte) (i >> 16));
	}

	/**
	 * 写入3字节无符号整数，大端序
	 *
	 * @param buffer ByteBuffer
	 * @param i      数据 (0-16777215)
	 */
	public static void writeMediumBE(ByteBuffer buffer, int i) {
		buffer.put((byte) (i >> 16));
		buffer.put((byte) (i >> 8));
		buffer.put((byte) i);
	}

	/**
	 * 写入4字节int，小端序
	 *
	 * @param buffer ByteBuffer
	 * @param i      数据
	 */
	public static void writeIntLE(ByteBuffer buffer, long i) {
		buffer.put((byte) i);
		buffer.put((byte) (i >> 8));
		buffer.put((byte) (i >> 16));
		buffer.put((byte) (i >> 24));
	}

	/**
	 * 写入4字节int，大端序
	 *
	 * @param buffer ByteBuffer
	 * @param i      数据
	 */
	public static void writeIntBE(ByteBuffer buffer, long i) {
		buffer.put((byte) (i >> 24));
		buffer.put((byte) (i >> 16));
		buffer.put((byte) (i >> 8));
		buffer.put((byte) i);
	}

	/**
	 * 写入4字节float，使用buffer当前字节序
	 *
	 * @param buffer ByteBuffer
	 * @param value  数据
	 */
	public static void writeFloat(ByteBuffer buffer, float value) {
		buffer.putFloat(value);
	}

	/**
	 * 写入4字节float，小端序
	 *
	 * @param buffer ByteBuffer
	 * @param value  数据
	 */
	public static void writeFloatLE(ByteBuffer buffer, float value) {
		writeIntLE(buffer, Float.floatToRawIntBits(value));
	}

	/**
	 * 写入4字节float，大端序
	 *
	 * @param buffer ByteBuffer
	 * @param value  数据
	 */
	public static void writeFloatBE(ByteBuffer buffer, float value) {
		writeIntBE(buffer, Float.floatToRawIntBits(value));
	}

	/**
	 * 写入8字节long，小端序
	 *
	 * @param buffer ByteBuffer
	 * @param l      数据
	 */
	public static void writeLongLE(ByteBuffer buffer, long l) {
		buffer.put((byte) l);
		buffer.put((byte) (l >> 8));
		buffer.put((byte) (l >> 16));
		buffer.put((byte) (l >> 24));
		buffer.put((byte) (l >> 32));
		buffer.put((byte) (l >> 40));
		buffer.put((byte) (l >> 48));
		buffer.put((byte) (l >> 56));
	}

	/**
	 * 写入8字节long，大端序
	 *
	 * @param buffer ByteBuffer
	 * @param l      数据
	 */
	public static void writeLongBE(ByteBuffer buffer, long l) {
		buffer.put((byte) (l >> 56));
		buffer.put((byte) (l >> 48));
		buffer.put((byte) (l >> 40));
		buffer.put((byte) (l >> 32));
		buffer.put((byte) (l >> 24));
		buffer.put((byte) (l >> 16));
		buffer.put((byte) (l >> 8));
		buffer.put((byte) l);
	}

	/**
	 * 写入8字节double，使用buffer当前字节序
	 *
	 * @param buffer ByteBuffer
	 * @param value  数据
	 */
	public static void writeDouble(ByteBuffer buffer, double value) {
		buffer.putDouble(value);
	}

	/**
	 * 写入8字节double，小端序
	 *
	 * @param buffer ByteBuffer
	 * @param value  数据
	 */
	public static void writeDoubleLE(ByteBuffer buffer, double value) {
		writeLongLE(buffer, Double.doubleToRawLongBits(value));
	}

	/**
	 * 写入8字节double，大端序
	 *
	 * @param buffer ByteBuffer
	 * @param value  数据
	 */
	public static void writeDoubleBE(ByteBuffer buffer, double value) {
		writeLongBE(buffer, Double.doubleToRawLongBits(value));
	}

	/**
	 * 跳过指定字节数，移动 position
	 *
	 * @param buffer ByteBuffer
	 * @param skip   要跳过的字节数
	 * @return buffer 自身
	 */
	public static ByteBuffer skipBytes(ByteBuffer buffer, int skip) {
		buffer.position(buffer.position() + skip);
		return buffer;
	}

	/**
	 * 组合两个 ByteBuffer 的可读部分为一个新的 ByteBuffer
	 *
	 * @param byteBuffer1 第一个 ByteBuffer
	 * @param byteBuffer2 第二个B yteBuffer
	 * @return 组合后的新 ByteBuffer
	 */
	public static ByteBuffer composite(ByteBuffer byteBuffer1, ByteBuffer byteBuffer2) {
		int capacity = byteBuffer1.remaining() + byteBuffer2.remaining();
		ByteBuffer ret = ByteBuffer.allocate(capacity);
		ret.put(byteBuffer1);
		ret.put(byteBuffer2);
		ret.rewind();
		return ret;
	}

	/**
	 * 克隆ByteBuffer，复制整个capacity的数据
	 *
	 * @param original 原ByteBuffer
	 * @return 新的ByteBuffer（堆缓冲区）
	 */
	public static ByteBuffer clone(ByteBuffer original) {
		ByteBuffer clone = ByteBuffer.allocate(original.capacity());
		original.rewind();
		clone.put(original);
		original.rewind();
		clone.flip();
		return clone;
	}

	/**
	 * 从源ByteBuffer复制指定区间到目标ByteBuffer
	 * <p>
	 * 自动处理堆缓冲区和直接缓冲区，不改变两个buffer的position和limit。
	 *
	 * @param src           源ByteBuffer
	 * @param srcStartIndex 源起始索引（从0开始）
	 * @param dst           目标ByteBuffer
	 * @param dstStartIndex 目标起始索引
	 * @param length        复制长度
	 */
	public static void copy(ByteBuffer src, int srcStartIndex, ByteBuffer dst, int dstStartIndex, int length) {
		if (src.hasArray() && dst.hasArray()) {
			// 堆缓冲区：直接使用System.arraycopy高效复制
			System.arraycopy(src.array(), src.arrayOffset() + srcStartIndex, dst.array(), dst.arrayOffset() + dstStartIndex, length);
		} else {
			// 直接缓冲区：使用slice避免改变原buffer的position/limit
			int oriSrcPos = src.position();
			int oriSrcLim = src.limit();
			int oriDstPos = dst.position();
			src.position(srcStartIndex);
			ByteBuffer srcSlice = src.slice();
			srcSlice.limit(length);
			src.position(oriSrcPos);
			src.limit(oriSrcLim);
			dst.position(dstStartIndex);
			dst.put(srcSlice);
			dst.position(oriDstPos);
		}
	}

	/**
	 * 复制ByteBuffer指定区间的数据到新缓冲区
	 * <p>
	 * 不改变原buffer的position和limit。
	 *
	 * @param src        源ByteBuffer
	 * @param startIndex 起始索引（从0开始）
	 * @param endIndex   结束索引
	 * @return 新的ByteBuffer（堆缓冲区）
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
	 * 复制ByteBuffer remaining区间的数据到新缓冲区
	 * <p>
	 * 不改变原buffer的position和limit。
	 *
	 * @param src 源ByteBuffer
	 * @return 新的ByteBuffer（堆缓冲区）
	 */
	public static ByteBuffer copy(ByteBuffer src) {
		int startIndex = src.position();
		int endIndex = src.limit();
		return copy(src, startIndex, endIndex);
	}

	/**
	 * 将ByteBuffer按指定单元大小拆分成多个ByteBuffer
	 * <p>
	 * 常用于拆包操作。
	 *
	 * @param src      源ByteBuffer
	 * @param unitSize 每个单元的大小
	 * @return 拆分后的ByteBuffer数组，如果不需要拆分则返回null
	 */
	public static ByteBuffer[] split(ByteBuffer src, int unitSize) {
		int limit = src.limit();
		if (unitSize >= limit) {
			return null;
		}
		int size = (int) Math.ceil((double) limit / unitSize);
		ByteBuffer[] ret = new ByteBuffer[size];
		int srcIndex = 0;
		for (int i = 0; i < size; i++) {
			int bufferSize = unitSize;
			// 最后一组可能小于unitSize
			if (i == size - 1) {
				bufferSize = src.remaining() % unitSize;
				if (bufferSize == 0) {
					bufferSize = unitSize;
				}
			}
			byte[] dest = new byte[bufferSize];
			System.arraycopy(src.array(), srcIndex, dest, 0, dest.length);
			srcIndex += bufferSize;

			ret[i] = ByteBuffer.wrap(dest);
			ret[i].position(0);
			ret[i].limit(ret[i].capacity());
		}
		return ret;
	}

	/**
	 * 查找行结束位置（\n或\r\n）
	 *
	 * @param buffer ByteBuffer
	 * @return 行结束索引，未找到返回-1
	 */
	public static int lineEnd(ByteBuffer buffer) {
		return lineEnd(buffer, Integer.MAX_VALUE);
	}

	/**
	 * 查找行结束位置（\n或\r\n），限制最大搜索长度
	 *
	 * @param buffer    ByteBuffer
	 * @param maxLength 最大搜索长度
	 * @return 行结束索引，未找到返回-1
	 */
	public static int lineEnd(ByteBuffer buffer, int maxLength) {
		int initPosition = buffer.position();
		int endPosition = indexOf(buffer, '\n', maxLength);
		// 处理\r\n情况
		if ((endPosition - initPosition > 0) && (buffer.get(endPosition - 1) == '\r')) {
			return endPosition - 1;
		}
		return endPosition;
	}

	/**
	 * 在ByteBuffer中查找指定字符的位置
	 * <p>
	 * 会改变buffer的position。查找范围受maxLength限制。
	 *
	 * @param buffer    ByteBuffer（position会被移动）
	 * @param theChar   要查找的字符
	 * @param maxLength 最大搜索长度
	 * @return 字符位置，未找到返回-1；超过maxLength抛出IndexOutOfBoundsException
	 */
	public static int indexOf(ByteBuffer buffer, char theChar, int maxLength) {
		int count = 0;
		// 只有remaining超过maxLength时才限制
		boolean needJudgeLengthOverflow = buffer.remaining() > maxLength;
		while (buffer.hasRemaining()) {
			if (buffer.get() == theChar) {
				return buffer.position() - 1;
			}
			if (needJudgeLengthOverflow) {
				count++;
				if (count >= maxLength) {
					throw new IndexOutOfBoundsException("maxlength is " + maxLength);
				}
			}
		}
		return -1;
	}

	/**
	 * 读取一行（默认UTF-8编码）
	 *
	 * @param buffer  ByteBuffer
	 * @param charset 字符编码
	 * @return 行字符串，未找到行尾返回null
	 */
	public static String readLine(ByteBuffer buffer, Charset charset) {
		return readLine(buffer, charset, Integer.MAX_VALUE);
	}

	/**
	 * 读取一行，限制最大长度
	 *
	 * @param buffer    ByteBuffer
	 * @param charset   字符编码
	 * @param maxLength 最大读取长度
	 * @return 行字符串，未找到行尾返回null
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
	 * 读取指定长度的字符串（默认UTF-8编码）
	 *
	 * @param buffer ByteBuffer
	 * @param count  字节长度
	 * @return 字符串
	 */
	public static String readString(ByteBuffer buffer, int count) {
		return readString(buffer, count, StandardCharsets.UTF_8);
	}

	/**
	 * 读取指定长度和编码的字符串
	 *
	 * @param buffer  ByteBuffer
	 * @param count   字节长度
	 * @param charset 字符编码
	 * @return 字符串
	 */
	public static String readString(ByteBuffer buffer, int count, Charset charset) {
		byte[] bytes = new byte[count];
		buffer.get(bytes);
		return new String(bytes, charset);
	}

	/**
	 * 读取到指定字符为止的字符串
	 *
	 * @param buffer    ByteBuffer
	 * @param charset   字符编码
	 * @param endChar   结束字符
	 * @param maxLength 最大读取长度
	 * @return 字符串，未找到结束字符返回null
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
	 * 将ByteBuffer remaining区间转为字符串（默认UTF-8编码）
	 *
	 * @param buffer ByteBuffer
	 * @return 字符串
	 */
	public static String toString(ByteBuffer buffer) {
		return toString(buffer, StandardCharsets.UTF_8);
	}

	/**
	 * 将ByteBuffer remaining区间转为指定编码的字符串
	 *
	 * @param buffer  ByteBuffer
	 * @param charset 字符编码
	 * @return 字符串
	 */
	public static String toString(ByteBuffer buffer, Charset charset) {
		return new String(toArray(buffer), charset);
	}

	/**
	 * 将byte数组转为字符串（默认UTF-8编码）
	 *
	 * @param buffer byte数组
	 * @return 字符串
	 */
	public static String toString(byte[] buffer) {
		return toString(buffer, StandardCharsets.UTF_8);
	}

	/**
	 * 将byte数组转为指定编码的字符串
	 *
	 * @param buffer  byte数组
	 * @param charset 字符编码
	 * @return 字符串
	 */
	public static String toString(byte[] buffer, Charset charset) {
		return new String(buffer, charset);
	}

	/**
	 * 以十六进制格式打印ByteBuffer
	 * <p>
	 * 不消耗buffer的position。
	 *
	 * @param byteBuffer ByteBuffer
	 * @return 十六进制字符串
	 */
	public static String hexDump(ByteBuffer byteBuffer) {
		return toHexString(toArray(byteBuffer));
	}

	/**
	 * 以十六进制格式打印字节数组
	 *
	 * @param bytes byte数组
	 * @return 十六进制字符串
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
		// 最后一行不足16字节时的填充
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
	 * 过滤控制字符，将0x00-0x1F范围内的字节替换为'.'（0x2E）
	 *
	 * @param bytes  byte数组
	 * @param offset 起始偏移
	 * @param count  字节数
	 * @return 过滤后的字符串
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
	 * 格式化十六进制字符串，补零到8位并加'h'后缀
	 *
	 * @param hexStr 十六进制字符串
	 * @return 格式化后的字符串，如"0000001Ah"
	 */
	private static String fixHexString(final String hexStr) {
		if (hexStr == null || hexStr.isEmpty()) {
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
