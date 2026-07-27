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
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DigestUtils 单元测试
 *
 * @author L.cm
 */
class DigestUtilsTest {

	private static final String TEXT = "mica 最牛逼";

	@Test
	void testMd5String() {
		// 已知 MD5: "mica 最牛逼" 的 md5 hex 值
		String md5Hex = DigestUtils.md5Hex(TEXT);
		assertEquals(32, md5Hex.length());
		assertEquals(md5Hex, DigestUtils.md5Hex(TEXT.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void testSha1() {
		String sha1 = DigestUtils.sha1Hex(TEXT);
		assertEquals(40, sha1.length());
		// 验证和 sha1(byte[]) 一致
		assertEquals(sha1, DigestUtils.sha1Hex(TEXT.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void testSha224() {
		String sha224 = DigestUtils.sha224Hex(TEXT);
		assertEquals(56, sha224.length());
	}

	@Test
	void testSha256() {
		String sha256 = DigestUtils.sha256Hex(TEXT);
		assertEquals(64, sha256.length());
	}

	@Test
	void testSha384() {
		String sha384 = DigestUtils.sha384Hex(TEXT);
		assertEquals(96, sha384.length());
	}

	@Test
	void testSha512() {
		String sha512 = DigestUtils.sha512Hex(TEXT);
		assertEquals(128, sha512.length());
	}

	@Test
	void testDigestAlgorithm() {
		String hex = DigestUtils.digestHex("SHA-256", "hello".getBytes(StandardCharsets.UTF_8));
		assertEquals(64, hex.length());
	}

	@Test
	void testHmacSha256() {
		String key = "secret-key";
		String hmac = DigestUtils.hmacSha256Hex(TEXT, key);
		assertEquals(64, hmac.length());
		// 相同的 key 和 data 应得到相同的结果
		String hmac2 = DigestUtils.hmacSha256Hex(TEXT.getBytes(StandardCharsets.UTF_8), key);
		assertEquals(hmac, hmac2);
	}

	@Test
	void testHmacSha1() {
		String key = "secret";
		String hmac = DigestUtils.hmacSha1Hex(TEXT, key);
		assertEquals(40, hmac.length());
	}

	@Test
	void testHmacSha512() {
		String key = "secret";
		String hmac = DigestUtils.hmacSha512Hex(TEXT, key);
		assertEquals(128, hmac.length());
	}

	@Test
	void testHmacMd5() {
		String key = "secret";
		String hmac = DigestUtils.hmacMd5Hex(TEXT, key);
		assertEquals(32, hmac.length());
	}

	@Test
	void testDigestHmacHex() {
		String key = "secret";
		String hmac = DigestUtils.digestHmacHex("HmacSHA256", TEXT, key);
		assertEquals(64, hmac.length());
	}

	@Test
	void testEncodeHexDecodeHex() {
		String original = "hello world 123";
		String hex = DigestUtils.encodeHex(original.getBytes(StandardCharsets.UTF_8));
		assertEquals(original, new String(DigestUtils.decodeHex(hex), StandardCharsets.UTF_8));
	}

	@Test
	void testSlowEquals() {
		String a = "hello world";
		String b = "hello world";
		String c = "hello WORLD";
		assertTrue(DigestUtils.slowEquals(a, b));
		assertFalse(DigestUtils.slowEquals(a, c));
		assertFalse(DigestUtils.slowEquals(a, null));
		assertFalse(DigestUtils.slowEquals(null, a));
		assertFalse(DigestUtils.slowEquals((String) null, null));
	}

	@Test
	void testSlowEqualsBytes() {
		byte[] a = "hello".getBytes(StandardCharsets.UTF_8);
		byte[] b = "hello".getBytes(StandardCharsets.UTF_8);
		byte[] c = "world".getBytes(StandardCharsets.UTF_8);
		byte[] d = "hell".getBytes(StandardCharsets.UTF_8);
		assertTrue(DigestUtils.slowEquals(a, b));
		assertFalse(DigestUtils.slowEquals(a, c));
		assertFalse(DigestUtils.slowEquals(a, d));
		assertFalse(DigestUtils.slowEquals(a, null));
		assertFalse(DigestUtils.slowEquals(null, a));
	}

	@Test
	void testUnknownAlgorithm() {
		// NoSuchAlgorithmException 会被 unchecked 包装重新抛出
		assertThrows(NoSuchAlgorithmException.class, () -> {
			DigestUtils.digest("UNKNOWN-ALG", new byte[]{1, 2, 3});
		});
	}
}
