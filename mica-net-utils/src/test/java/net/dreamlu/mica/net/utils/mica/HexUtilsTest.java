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

package net.dreamlu.mica.net.utils.mica;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HexUtils 单元测试
 *
 * @author L.cm
 */
class HexUtilsTest {

	@Test
	void testEncodeDecodeRoundTrip() {
		String text = "mica 最牛逼";
		String hex = "6d69636120e69c80e7899be980bc";
		String hexText = HexUtils.encodeToString(text);
		assertEquals(hex, hexText);

		String decode = HexUtils.decodeToString(hexText);
		assertEquals(text, decode);
		assertEquals("mica 最牛逼", HexUtils.decodeToString(hex));
	}

	@Test
	void testEncodeByteArrayLowerCase() {
		byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
		String lower = HexUtils.encodeToString(data, true);
		assertEquals("616263", lower);
	}

	@Test
	void testEncodeByteArrayUpperCase() {
		byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
		String upper = HexUtils.encodeToString(data, false);
		assertEquals("616263".toUpperCase(), upper);
	}

	@Test
	void testEncodeSingleByte() {
		// 单字节编码
		assertEquals("00", HexUtils.encode((byte) 0x00));
		assertEquals("0a", HexUtils.encode((byte) 0x0a));
		assertEquals("ff", HexUtils.encode((byte) 0xff));
		assertEquals("7f", HexUtils.encode((byte) 0x7f));
	}

	@Test
	void testEncodeNullString() {
		assertNull(HexUtils.encodeToString((String) null));
		assertNull(HexUtils.encodeToString(""));
		assertNull(HexUtils.encodeToString("   "));
	}

	@Test
	void testDecodeNull() {
		assertNull(HexUtils.decode(""));
		assertNull(HexUtils.decode((String) null));
		assertNull(HexUtils.decodeToString(""));
		assertNull(HexUtils.decodeToString((String) null));
	}

	@Test
	void testDecodeInvalidLength() {
		// 奇数长度应该抛异常
		assertThrows(IllegalArgumentException.class, () -> HexUtils.decode("abc"));
	}

	@Test
	void testDecodeInvalidCharacter() {
		assertThrows(IllegalArgumentException.class, () -> HexUtils.decode("zz"));
	}

	@Test
	void testDecodeRoundTrip() {
		byte[] data = new byte[256];
		for (int i = 0; i < 256; i++) {
			data[i] = (byte) i;
		}
		String hex = HexUtils.encodeToString(data);
		byte[] decoded = HexUtils.decode(hex);
		assertArrayEquals(data, decoded);
	}

	@Test
	void testHashFNV1() {
		// FNV1 已知向量验证
		String text = "abc";
		byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
		int hash = HexUtils.hashFNV1(bytes);
		assertEquals(0x1A47E90B, hash);

		// offset/len 模式
		byte[] data = "xxabc".getBytes(StandardCharsets.UTF_8);
		int hashRange = HexUtils.hashFNV1(data, 2, 3);
		assertEquals(hash, hashRange);
	}

	@Test
	void testHashFNV1Empty() {
		int hash = HexUtils.hashFNV1(new byte[0]);
		// OFFSET_BASIS = (int) 2166136261L，由于 int 范围溢出，实际值为 -2128831035
		assertEquals((int) 2166136261L, hash);
	}
}
