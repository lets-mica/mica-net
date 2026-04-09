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

/**
 * ProxyProtocol v2 解析测试
 *
 * @author L.cm
 */
class ProxyProtocolDecoderV2Test {

	/**
	 * V2 签名: \x0D \x0A \x0D \x0A \x00 \x0D \x0A \x51 \x55 \x49 \x54 \x0A
	 */
	private static final byte[] V2_SIGNATURE = new byte[] {
		0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A
	};

	/**
	 * 构建 V2 TCP over IPv4 头
	 * 源: 192.168.0.1:56324, 目标: 192.168.0.11:443
	 */
	@Test
	void testV2Tcp4Decode() throws TioDecodeException {
		ByteBuffer buffer = buildV2Tcp4Header(
			new byte[]{(byte) 192, (byte) 168, 0, 1},
			new byte[]{(byte) 192, (byte) 168, 0, 11},
			(short) 56324,
			(short) 443
		);

		ProxyProtocolMessage msg = ProxyProtocolDecoder.decodeV2ForTest(buffer, buffer.limit());

		Assertions.assertEquals("TCP4", msg.getProtocol());
		Assertions.assertEquals("192.168.0.1", msg.getSourceAddress());
		Assertions.assertEquals("192.168.0.11", msg.getDestinationAddress());
		Assertions.assertEquals(56324, msg.getSourcePort());
		Assertions.assertEquals(443, msg.getDestinationPort());
	}

	/**
	 * 构建 V2 UDP over IPv4 头
	 */
	@Test
	void testV2Udp4Decode() throws TioDecodeException {
		ByteBuffer buffer = buildV2Udp4Header(
			new byte[]{10, 0, 0, 1},
			new byte[]{10, 0, 0, 2},
			(short) 12345,
			(short) 80
		);

		ProxyProtocolMessage msg = ProxyProtocolDecoder.decodeV2ForTest(buffer, buffer.limit());

		Assertions.assertEquals("UDP4", msg.getProtocol());
		Assertions.assertEquals("10.0.0.1", msg.getSourceAddress());
		Assertions.assertEquals("10.0.0.2", msg.getDestinationAddress());
		Assertions.assertEquals(12345, msg.getSourcePort());
		Assertions.assertEquals(80, msg.getDestinationPort());
	}

	/**
	 * 构建 V2 TCP over IPv6 头
	 * 源: 2001:db8::1:56324, 目标: 2001:db8::2:443
	 */
	@Test
	void testV2Tcp6Decode() throws TioDecodeException {
		// 2001:0db8:0000:0000:0000:0000:0000:0001 -> 16 bytes
		byte[] srcAddr = new byte[]{
			0x20, 0x01, 0x0d, (byte) 0xb8, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
		};
		// 2001:0db8:0000:0000:0000:0000:0000:0002
		byte[] dstAddr = new byte[]{
			0x20, 0x01, 0x0d, (byte) 0xb8, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02
		};

		ByteBuffer buffer = buildV2Tcp6Header(srcAddr, dstAddr, (short) 56324, (short) 443);

		ProxyProtocolMessage msg = ProxyProtocolDecoder.decodeV2ForTest(buffer, buffer.limit());

		Assertions.assertEquals("TCP6", msg.getProtocol());
		// 简化格式: 2001:db8::1
		Assertions.assertEquals("2001:db8:0:0:0:0:0:1", msg.getSourceAddress());
		Assertions.assertEquals("2001:db8:0:0:0:0:0:2", msg.getDestinationAddress());
		Assertions.assertEquals(56324, msg.getSourcePort());
		Assertions.assertEquals(443, msg.getDestinationPort());
	}

	/**
	 * V2 LOCAL 命令测试 (健康检查)
	 */
	@Test
	void testV2LocalCommand() throws TioDecodeException {
		ByteBuffer buffer = buildV2LocalHeader();

		ProxyProtocolMessage msg = ProxyProtocolDecoder.decodeV2ForTest(buffer, buffer.limit());

		Assertions.assertEquals("LOCAL", msg.getProtocol());
		Assertions.assertNull(msg.getSourceAddress());
		Assertions.assertNull(msg.getDestinationAddress());
		Assertions.assertEquals(0, msg.getSourcePort());
		Assertions.assertEquals(0, msg.getDestinationPort());
	}

	/**
	 * V2 UNSPEC 命令测试
	 */
	@Test
	void testV2UnspecCommand() throws TioDecodeException {
		ByteBuffer buffer = buildV2UnspecHeader();

		ProxyProtocolMessage msg = ProxyProtocolDecoder.decodeV2ForTest(buffer, buffer.limit());

		Assertions.assertEquals("UNSPEC", msg.getProtocol());
		Assertions.assertNull(msg.getSourceAddress());
		Assertions.assertNull(msg.getDestinationAddress());
	}

	/**
	 * V2 无效签名测试
	 */
	@Test
	void testV2InvalidSignature() {
		// "NOT A SIGNATURE" 是 16 字节，加上后面的 header 字段
		ByteBuffer buffer = ByteBuffer.allocate(32);
		buffer.put("NOT A SIGNATURE".getBytes());
		buffer.put(new byte[]{0x11}); // ver_cmd
		buffer.put(new byte[]{0x11}); // fam
		buffer.putShort((short) 12);  // addr len
		buffer.put(new byte[12]);     // addr data
		buffer.flip();

		Assertions.assertThrows(TioDecodeException.class, () -> {
			ProxyProtocolDecoder.decodeV2ForTest(buffer, buffer.limit());
		});
	}

	/**
	 * V2 无效版本测试
	 */
	@Test
	void testV2InvalidVersion() {
		// 12 bytes sig + ver_cmd + fam + 2 bytes len + 12 bytes addr = 28
		ByteBuffer buffer = ByteBuffer.allocate(28);
		buffer.put(V2_SIGNATURE);
		buffer.put((byte) 0x30); // version 3 (invalid)
		buffer.put((byte) 0x11); // fam
		buffer.putShort((short) 12);
		buffer.put(new byte[12]);
		buffer.flip();

		Assertions.assertThrows(TioDecodeException.class, () -> {
			ProxyProtocolDecoder.decodeV2ForTest(buffer, buffer.limit());
		});
	}

	/**
	 * V2 无效命令测试
	 */
	@Test
	void testV2InvalidCommand() {
		// 12 bytes sig + ver_cmd + fam + 2 bytes len + 12 bytes addr = 28
		ByteBuffer buffer = ByteBuffer.allocate(28);
		buffer.put(V2_SIGNATURE);
		buffer.put((byte) 0x22); // version 2, command 2 (invalid)
		buffer.put((byte) 0x11); // fam
		buffer.putShort((short) 12);
		buffer.put(new byte[12]);
		buffer.flip();

		Assertions.assertThrows(TioDecodeException.class, () -> {
			ProxyProtocolDecoder.decodeV2ForTest(buffer, buffer.limit());
		});
	}

	/**
	 * V2 无效地址族测试
	 */
	@Test
	void testV2InvalidAddressFamily() {
		// 12 bytes sig + ver_cmd + fam + 2 bytes len + 12 bytes addr = 28
		ByteBuffer buffer = ByteBuffer.allocate(28);
		buffer.put(V2_SIGNATURE);
		buffer.put((byte) 0x21); // version 2, command 1 (PROXY)
		buffer.put((byte) 0x40); // fam 0x40 (invalid)
		buffer.putShort((short) 12);
		buffer.put(new byte[12]);
		buffer.flip();

		Assertions.assertThrows(TioDecodeException.class, () -> {
			ProxyProtocolDecoder.decodeV2ForTest(buffer, buffer.limit());
		});
	}

	// ==================== 半包测试 ====================

	/**
	 * V2 半包测试：签名不完整
	 */
	@Test
	void testV2HalfPacketSignatureIncomplete() {
		ByteBuffer buffer = ByteBuffer.allocate(10);
		buffer.put(V2_SIGNATURE, 0, 10);
		buffer.flip();

		Assertions.assertThrows(TioDecodeException.class, () -> {
			ProxyProtocolDecoder.decodeV2ForTest(buffer, buffer.limit());
		});
	}

	/**
	 * V2 半包测试：只有签名，缺少头部字段
	 */
	@Test
	void testV2HalfPacketSignatureOnly() {
		ByteBuffer buffer = ByteBuffer.allocate(12);
		buffer.put(V2_SIGNATURE);
		buffer.flip();

		Assertions.assertThrows(TioDecodeException.class, () -> {
			ProxyProtocolDecoder.decodeV2ForTest(buffer, buffer.limit());
		});
	}

	/**
	 * V2 半包测试：签名+ver_cmd，缺少fam和addrLen
	 */
	@Test
	void testV2HalfPacketMissingFamAndAddrLen() {
		ByteBuffer buffer = ByteBuffer.allocate(13);
		buffer.put(V2_SIGNATURE);
		buffer.put((byte) 0x21); // ver_cmd
		buffer.flip();

		Assertions.assertThrows(TioDecodeException.class, () -> {
			ProxyProtocolDecoder.decodeV2ForTest(buffer, buffer.limit());
		});
	}

	/**
	 * V2 半包测试：签名+ver_cmd+fam，缺少addrLen
	 */
	@Test
	void testV2HalfPacketMissingAddrLen() {
		ByteBuffer buffer = ByteBuffer.allocate(14);
		buffer.put(V2_SIGNATURE);
		buffer.put((byte) 0x21); // ver_cmd
		buffer.put((byte) 0x11); // fam (TCP4)
		buffer.flip();

		Assertions.assertThrows(TioDecodeException.class, () -> {
			ProxyProtocolDecoder.decodeV2ForTest(buffer, buffer.limit());
		});
	}

	/**
	 * V2 半包测试：完整头但地址数据不完整
	 */
	@Test
	void testV2HalfPacketIncompleteAddress() {
		// 完整头(16) + 部分地址(8字节，但需要12字节)
		ByteBuffer buffer = ByteBuffer.allocate(24);
		buffer.put(V2_SIGNATURE);
		buffer.put((byte) 0x21); // ver_cmd
		buffer.put((byte) 0x11); // fam (TCP4)
		buffer.putShort((short) 12); // addrLen = 12
		// 只放8字节地址（需要12字节）
		buffer.put(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
		buffer.flip();

		Assertions.assertThrows(TioDecodeException.class, () -> {
			ProxyProtocolDecoder.decodeV2ForTest(buffer, buffer.limit());
		});
	}

	// ============ 辅助方法 ============

	/**
	 * 构建 V2 TCP over IPv4 头
	 */
	private ByteBuffer buildV2Tcp4Header(byte[] srcAddr, byte[] dstAddr, short srcPort, short dstPort) {
		byte fam = 0x11; // IPv4 + TCP
		short addrLen = 12; // 4+4+2+2
		int totalLen = 16 + addrLen;

		ByteBuffer buffer = ByteBuffer.allocate(totalLen);
		buffer.put(V2_SIGNATURE);
		buffer.put((byte) 0x21); // version 2, PROXY command
		buffer.put(fam);
		buffer.putShort(addrLen);
		buffer.put(srcAddr);
		buffer.put(dstAddr);
		buffer.putShort(srcPort);
		buffer.putShort(dstPort);
		buffer.flip();

		return buffer;
	}

	/**
	 * 构建 V2 UDP over IPv4 头
	 */
	private ByteBuffer buildV2Udp4Header(byte[] srcAddr, byte[] dstAddr, short srcPort, short dstPort) {
		byte fam = 0x12; // IPv4 + UDP
		short addrLen = 12; // 4+4+2+2
		int totalLen = 16 + addrLen;

		ByteBuffer buffer = ByteBuffer.allocate(totalLen);
		buffer.put(V2_SIGNATURE);
		buffer.put((byte) 0x21); // version 2, PROXY command
		buffer.put(fam);
		buffer.putShort(addrLen);
		buffer.put(srcAddr);
		buffer.put(dstAddr);
		buffer.putShort(srcPort);
		buffer.putShort(dstPort);
		buffer.flip();

		return buffer;
	}

	/**
	 * 构建 V2 TCP over IPv6 头
	 */
	private ByteBuffer buildV2Tcp6Header(byte[] srcAddr, byte[] dstAddr, short srcPort, short dstPort) {
		byte fam = 0x21; // IPv6 + TCP
		short addrLen = 36; // 16+16+2+2
		int totalLen = 16 + addrLen;

		ByteBuffer buffer = ByteBuffer.allocate(totalLen);
		buffer.put(V2_SIGNATURE);
		buffer.put((byte) 0x21); // version 2, PROXY command
		buffer.put(fam);
		buffer.putShort(addrLen);
		buffer.put(srcAddr);
		buffer.put(dstAddr);
		buffer.putShort(srcPort);
		buffer.putShort(dstPort);
		buffer.flip();

		return buffer;
	}

	/**
	 * 构建 V2 LOCAL 头
	 */
	private ByteBuffer buildV2LocalHeader() {
		ByteBuffer buffer = ByteBuffer.allocate(16);
		buffer.put(V2_SIGNATURE);
		buffer.put((byte) 0x20); // version 2, LOCAL command
		buffer.put((byte) 0x00); // UNSPEC family
		buffer.putShort((short) 0); // addr len = 0
		buffer.flip();

		return buffer;
	}

	/**
	 * 构建 V2 UNSPEC 头
	 */
	private ByteBuffer buildV2UnspecHeader() {
		ByteBuffer buffer = ByteBuffer.allocate(16);
		buffer.put(V2_SIGNATURE);
		buffer.put((byte) 0x21); // version 2, PROXY command
		buffer.put((byte) 0x00); // UNSPEC family
		buffer.putShort((short) 0); // addr len = 0
		buffer.flip();

		return buffer;
	}
}
