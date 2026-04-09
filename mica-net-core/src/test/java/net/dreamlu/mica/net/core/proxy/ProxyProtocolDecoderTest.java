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

package net.dreamlu.mica.net.core.proxy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import net.dreamlu.mica.net.core.exception.TioDecodeException;
import net.dreamlu.mica.net.server.proxy.ProxyProtocolDecoder;
import net.dreamlu.mica.net.server.proxy.ProxyProtocolMessage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * ProxyProtocol 解析测试
 *
 * @author L.cm
 */
class ProxyProtocolDecoderTest {

	@Test
	void testIPV4Decode() throws TioDecodeException {
		String header = "PROXY TCP4 192.168.0.1 192.168.0.11 56324 443\r\n";
		ByteBuffer byteBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.US_ASCII));
		ProxyProtocolMessage msg = ProxyProtocolDecoder.decodeForTest(byteBuffer, byteBuffer.limit());
		Assertions.assertEquals("TCP4", msg.getProtocol());
		Assertions.assertEquals("192.168.0.1", msg.getSourceAddress());
		Assertions.assertEquals("192.168.0.11", msg.getDestinationAddress());
		Assertions.assertEquals(56324, msg.getSourcePort());
		Assertions.assertEquals(443, msg.getDestinationPort());
	}

	@Test
	void testIPV6Decode() throws TioDecodeException {
		String header = "PROXY TCP6 2001:0db8:85a3:0000:0000:8a2e:0370:7334 1050:0:0:0:5:600:300c:326b 56324 443\r\n";
		ByteBuffer byteBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.US_ASCII));
		ProxyProtocolMessage msg = ProxyProtocolDecoder.decodeForTest(byteBuffer, byteBuffer.limit());
		Assertions.assertEquals("TCP6", msg.getProtocol());
		Assertions.assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", msg.getSourceAddress());
		Assertions.assertEquals("1050:0:0:0:5:600:300c:326b", msg.getDestinationAddress());
		Assertions.assertEquals(56324, msg.getSourcePort());
		Assertions.assertEquals(443, msg.getDestinationPort());
	}

	@Test
	void testUnknownProtocolDecode() throws TioDecodeException {
		String header = "PROXY UNKNOWN 192.168.0.1 192.168.0.11 56324 443\r\n";
		ByteBuffer byteBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.US_ASCII));
		ProxyProtocolMessage msg = ProxyProtocolDecoder.decodeForTest(byteBuffer, byteBuffer.limit());
		Assertions.assertEquals("UNKNOWN", msg.getProtocol());
		Assertions.assertNull(msg.getSourceAddress());
		Assertions.assertNull(msg.getDestinationAddress());
		Assertions.assertEquals(0, msg.getSourcePort());
		Assertions.assertEquals(0, msg.getDestinationPort());
	}

	@Test
	void testV1NoUDP() {
		final String header = "PROXY UDP4 192.168.0.1 192.168.0.11 56324 443\r\n";
		Assertions.assertThrows(TioDecodeException.class, () -> {
			ByteBuffer byteBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.US_ASCII));
			ProxyProtocolDecoder.decodeForTest(byteBuffer, byteBuffer.limit());
		});
	}

	@Test
	void testInvalidPort() {
		final String header = "PROXY TCP4 192.168.0.1 192.168.0.11 80000 443\r\n";
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ByteBuffer byteBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.US_ASCII));
			ProxyProtocolDecoder.decodeForTest(byteBuffer, byteBuffer.limit());
		});
	}

	@Test
	void testInvalidProtocol() {
		final String header = "PROXY TCP7 192.168.0.1 192.168.0.11 56324 443\r\n";
		Assertions.assertThrows(TioDecodeException.class, () -> {
			ByteBuffer byteBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.US_ASCII));
			ProxyProtocolDecoder.decodeForTest(byteBuffer, byteBuffer.limit());
		});
	}

	@Test
	void testMissingParams() {
		final String header = "PROXY TCP4 192.168.0.1 192.168.0.11 56324\r\n";
		Assertions.assertThrows(TioDecodeException.class, () -> {
			ByteBuffer byteBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.US_ASCII));
			ProxyProtocolDecoder.decodeForTest(byteBuffer, byteBuffer.limit());
		});
	}

	@Test
	void testTooManyParams() {
		final String header = "PROXY TCP4 192.168.0.1 192.168.0.11 56324 443 123\r\n";
		Assertions.assertThrows(TioDecodeException.class, () -> {
			ByteBuffer byteBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.US_ASCII));
			ProxyProtocolDecoder.decodeForTest(byteBuffer, byteBuffer.limit());
		});
	}

	@Test
	void testInvalidCommand() {
		final String header = "PING TCP4 192.168.0.1 192.168.0.11 56324 443\r\n";
		Assertions.assertThrows(TioDecodeException.class, () -> {
			ByteBuffer byteBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.US_ASCII));
			ProxyProtocolDecoder.decodeForTest(byteBuffer, byteBuffer.limit());
		});
	}

	@Test
	void testInvalidEOL() {
		final String header = "PROXY TCP4 192.168.0.1 192.168.0.11 56324 443\nGET / HTTP/1.1\r\n";
		Assertions.assertThrows(TioDecodeException.class, () -> {
			ByteBuffer byteBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.US_ASCII));
			ProxyProtocolDecoder.decodeForTest(byteBuffer, byteBuffer.limit());
		});
	}

	@Test
	void testHeaderTooLong() {
		final String header = "PROXY TCP4 192.168.0.1 192.168.0.11 56324 " +
			"00000000000000000000000000000000000000000000000000000000000000000443\r\n";
		Assertions.assertThrows(TioDecodeException.class, () -> {
			ByteBuffer byteBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.US_ASCII));
			ProxyProtocolDecoder.decodeForTest(byteBuffer, byteBuffer.limit());
		});
	}

	// ==================== 半包测试 ====================

	/**
	 * V1 半包测试：只有前缀，没有完整行
	 */
	@Test
	void testV1HalfPacketPrefixOnly() throws TioDecodeException {
		final String header = "PROXY ";
		ByteBuffer byteBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.US_ASCII));
		ProxyProtocolMessage msg = ProxyProtocolDecoder.decodeForTest(byteBuffer, byteBuffer.limit());
		Assertions.assertNull(msg);
	}

	/**
	 * V1 半包测试：有前缀和协议类型，缺少地址信息
	 */
	@Test
	void testV1HalfPacketMissingAddresses() throws TioDecodeException {
		final String header = "PROXY TCP4 192.168.0.1";
		ByteBuffer byteBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.US_ASCII));
		ProxyProtocolMessage msg = ProxyProtocolDecoder.decodeForTest(byteBuffer, byteBuffer.limit());
		Assertions.assertNull(msg);
	}

	/**
	 * V1 半包测试：有完整地址但缺少 \r\n
	 */
	@Test
	void testV1HalfPacketMissingCRLF() throws TioDecodeException {
		final String header = "PROXY TCP4 192.168.0.1 192.168.0.11 56324 443\n";
		ByteBuffer byteBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.US_ASCII));
		ProxyProtocolMessage msg = ProxyProtocolDecoder.decodeForTest(byteBuffer, byteBuffer.limit());
		Assertions.assertNull(msg);
	}

	/**
	 * V1 半包测试：有 \r 但没有 \n
	 */
	@Test
	void testV1HalfPacketMissingLF() throws TioDecodeException {
		final String header = "PROXY TCP4 192.168.0.1 192.168.0.11 56324 443\r";
		ByteBuffer byteBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.US_ASCII));
		ProxyProtocolMessage msg = ProxyProtocolDecoder.decodeForTest(byteBuffer, byteBuffer.limit());
		Assertions.assertNull(msg);
	}

}
