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
	 * read unsigned short，2个字节无符号
	 *
	 * @param buffer ByteBuffer
	 * @return int
	 */
	public static int readUnsignedShort(ByteBuffer buffer) {
		int i = buffer.get() & 0xff;
		i |= (buffer.get() & 0xff) << 8;
		return i;
	}

	/**
	 * read unsigned 3个字节无符号
	 *
	 * @param buffer ByteBuffer
	 * @return int
	 */
	public static int readUnsignedMedium(ByteBuffer buffer) {
		int i = buffer.get() & 0xff;
		i |= (buffer.get() & 0xff) << 8;
		i |= (buffer.get() & 0xff) << 16;
		return i;
	}

	/**
	 * read unsigned int, 4个字节无符号
	 *
	 * @param buffer ByteBuffer
	 * @return long
	 */
	public static long readUnsignedInt(ByteBuffer buffer) {
		long l = buffer.get() & 0xff;
		l |= (buffer.get() & 0xff) << 8;
		l |= (buffer.get() & 0xff) << 16;
		l |= (long) (buffer.get() & 0xff) << 24;
		return l;
	}

	/**
	 * read unsigned byte
	 *
	 * @param buffer ByteBuffer
	 * @return String
	 */
	public static String readString(ByteBuffer buffer, int count) {
		return readString(buffer, count, StandardCharsets.UTF_8);
	}

	/**
	 * read unsigned byte
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
