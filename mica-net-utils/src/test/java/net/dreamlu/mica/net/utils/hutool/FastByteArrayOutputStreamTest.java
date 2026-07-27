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

package net.dreamlu.mica.net.utils.hutool;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FastByteArrayOutputStream 单元测试
 *
 * @author L.cm
 */
class FastByteArrayOutputStreamTest {

	@Test
	void testWriteSingleByte() {
		FastByteArrayOutputStream out = new FastByteArrayOutputStream();
		out.write('a');
		out.write('b');
		out.write('c');
		assertEquals(3, out.size());
		assertArrayEquals(new byte[]{'a', 'b', 'c'}, out.toByteArray());
	}

	@Test
	void testWriteByteArray() throws IOException {
		FastByteArrayOutputStream out = new FastByteArrayOutputStream();
		byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
		out.write(data, 0, data.length);
		assertEquals(data.length, out.size());
		assertArrayEquals(data, out.toByteArray());
	}

	@Test
	void testWriteByteArrayOffset() throws IOException {
		FastByteArrayOutputStream out = new FastByteArrayOutputStream();
		byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
		out.write(data, 6, 5);
		assertEquals(5, out.size());
		assertEquals("world", out.toString());
	}

	@Test
	void testReset() throws IOException {
		FastByteArrayOutputStream out = new FastByteArrayOutputStream();
		out.write("hello".getBytes(StandardCharsets.UTF_8), 0, 5);
		assertEquals(5, out.size());
		out.reset();
		assertEquals(0, out.size());
		assertArrayEquals(new byte[0], out.toByteArray());
	}

	@Test
	void testToString() throws IOException {
		FastByteArrayOutputStream out = new FastByteArrayOutputStream();
		String text = "Hello, mica!";
		out.write(text.getBytes(StandardCharsets.UTF_8), 0, text.length());
		assertEquals(text, out.toString());
	}

	@Test
	void testToStringWithCharset() throws IOException {
		FastByteArrayOutputStream out = new FastByteArrayOutputStream();
		String text = "你好 mica";
		out.write(text.getBytes(StandardCharsets.UTF_8), 0, text.getBytes(StandardCharsets.UTF_8).length);
		assertEquals(text, out.toString(StandardCharsets.UTF_8));
		assertEquals(text, out.toString("UTF-8"));
	}

	@Test
	void testWriteTo() throws IOException {
		FastByteArrayOutputStream out = new FastByteArrayOutputStream();
		byte[] part1 = "hello ".getBytes(StandardCharsets.UTF_8);
		byte[] part2 = "world".getBytes(StandardCharsets.UTF_8);
		out.write(part1);
		out.write(part2);

		ByteArrayOutputStream sink = new ByteArrayOutputStream();
		out.writeTo(sink);
		assertArrayEquals(new byte[]{'h', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd'}, sink.toByteArray());
	}

	@Test
	void testCloseDoesNotThrow() {
		FastByteArrayOutputStream out = new FastByteArrayOutputStream();
		assertDoesNotThrow(out::close);
		// close 后仍可继续写
		out.write(1);
		assertEquals(1, out.size());
	}

	@Test
	void testGrowth() throws IOException {
		// 初始 1024 容量，超出后自动扩容
		FastByteArrayOutputStream out = new FastByteArrayOutputStream(16);
		byte[] large = new byte[2048];
		for (int i = 0; i < large.length; i++) {
			large[i] = (byte) (i % 256);
		}
		out.write(large, 0, large.length);
		assertEquals(2048, out.size());
		assertArrayEquals(large, out.toByteArray());
	}
}