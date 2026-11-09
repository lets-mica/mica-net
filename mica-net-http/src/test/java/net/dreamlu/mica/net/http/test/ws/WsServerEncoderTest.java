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

package net.dreamlu.mica.net.http.test.ws;

import net.dreamlu.mica.net.websocket.common.Opcode;
import net.dreamlu.mica.net.websocket.common.WsCloseCode;
import net.dreamlu.mica.net.websocket.common.WsResponse;
import net.dreamlu.mica.net.websocket.common.WsServerEncoder;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WsServerEncoder 单元测试，校验 RFC 6455 §5.2 的编码要求。
 *
 * @author L.cm
 */
class WsServerEncoderTest {

	@Test
	void encode_shortPayload_uses7BitLength() {
		WsResponse response = WsResponse.fromText("hello", StandardCharsets.UTF_8);

		ByteBuffer buffer = WsServerEncoder.encode(response);
		buffer.flip();

		assertEquals(2 + 5, buffer.remaining());
		assertEquals(0x81, buffer.get(0) & 0xFF);
		assertEquals(5, buffer.get(1) & 0xFF);
	}

	@Test
	void encode_mediumPayload_uses16BitLength() {
		byte[] payload = new byte[1000];
		WsResponse response = WsResponse.fromBytes(payload);

		ByteBuffer buffer = WsServerEncoder.encode(response);
		buffer.flip();

		// 2 字节头 + 2 字节长度 + 负载
		assertEquals(4 + 1000, buffer.remaining());
		assertEquals(126, buffer.get(1) & 0xFF);
		assertEquals(1000, ((buffer.get(2) & 0xFF) << 8) | (buffer.get(3) & 0xFF));
	}

	@Test
	void encode_boundaryPayload_uses16BitLength() {
		// 65535 是 16 位长度能表示的最大值，仍应使用 16 位长度
		byte[] payload = new byte[65535];
		WsResponse response = WsResponse.fromBytes(payload);

		ByteBuffer buffer = WsServerEncoder.encode(response);
		buffer.flip();

		assertEquals(4 + 65535, buffer.remaining());
		assertEquals(126, buffer.get(1) & 0xFF);
	}

	@Test
	void encode_largePayload_uses64BitLength() {
		byte[] payload = new byte[65536];
		WsResponse response = WsResponse.fromBytes(payload);

		ByteBuffer buffer = WsServerEncoder.encode(response);
		buffer.flip();

		assertEquals(10 + 65536, buffer.remaining());
		assertEquals(127, buffer.get(1) & 0xFF);
	}

	@Test
	void encode_doesNotSetMaskBit() {
		// 服务端发往客户端的帧不允许掩码
		WsResponse response = WsResponse.fromText("hello", StandardCharsets.UTF_8);

		ByteBuffer buffer = WsServerEncoder.encode(response);
		buffer.flip();

		assertEquals(0, buffer.get(1) & 0x80);
	}

	@Test
	void encode_setsFinBit() {
		WsResponse response = WsResponse.fromText("hello", StandardCharsets.UTF_8);

		ByteBuffer buffer = WsServerEncoder.encode(response);
		buffer.flip();

		assertTrue((buffer.get(0) & 0x80) != 0);
	}

	@Test
	void encode_closeFrame_containsStatusCodeAndReason() {
		WsResponse response = WsResponse.fromClose(WsCloseCode.NORMAL, "bye", StandardCharsets.UTF_8);

		ByteBuffer buffer = WsServerEncoder.encode(response);
		buffer.flip();

		assertEquals(Opcode.CLOSE.getCode(), buffer.get(0) & 0x0F);
		assertEquals(5, buffer.get(1) & 0xFF);
		assertEquals(0x03, buffer.get(2) & 0xFF);
		assertEquals(0xE8, buffer.get(3) & 0xFF);
		assertEquals('b', buffer.get(4) & 0xFF);
		assertEquals('y', buffer.get(5) & 0xFF);
		assertEquals('e', buffer.get(6) & 0xFF);
	}

	@Test
	void encode_emptyCloseFrame_hasNoPayload() {
		WsResponse response = WsResponse.fromClose();

		ByteBuffer buffer = WsServerEncoder.encode(response);
		buffer.flip();

		assertEquals(Opcode.CLOSE.getCode(), buffer.get(0) & 0x0F);
		assertEquals(0, buffer.get(1) & 0xFF);
		assertEquals(2, buffer.remaining());
	}

	@Test
	void encode_pingPayloadOver125Bytes_isRejected() {
		// RFC 6455 §5.5：控制帧负载不能超过 125 字节，超了就是非法帧
		WsResponse response = WsResponse.fromPing(new byte[126]);

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> WsServerEncoder.encode(response));
		assertTrue(e.getMessage().contains("125"));
	}

	@Test
	void encode_pingPayload125Bytes_isAllowed() {
		WsResponse response = WsResponse.fromPing(new byte[125]);

		ByteBuffer buffer = WsServerEncoder.encode(response);
		buffer.flip();

		assertEquals(Opcode.PING.getCode(), buffer.get(0) & 0x0F);
		assertEquals(125, buffer.get(1) & 0xFF);
		assertEquals(127, buffer.remaining());
	}
}
