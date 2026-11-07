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
import net.dreamlu.mica.net.websocket.common.WsPacket;
import net.dreamlu.mica.net.websocket.common.WsProtocolException;
import net.dreamlu.mica.net.websocket.common.WsRequest;
import net.dreamlu.mica.net.websocket.common.WsServerDecoder;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WsServerDecoder 单元测试，校验 RFC 6455 §5.2 的帧格式要求。
 *
 * @author L.cm
 */
class WsServerDecoderTest {

	@Test
	void decode_maskedTextFrame_returnsPayload() throws Exception {
		byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.wrap(WsTestClient.buildFrame(0x1, payload, true, true));

		WsRequest request = WsServerDecoder.decode(buffer, null);

		assertNotNull(request);
		assertEquals(Opcode.TEXT, request.getWsOpcode());
		assertTrue(request.isWsEof());
		assertArrayEquals(payload, request.getBody());
	}

	@Test
	void decode_incompleteFrame_returnsNull() throws Exception {
		byte[] frame = WsTestClient.buildFrame(0x1, "hello".getBytes(StandardCharsets.UTF_8), true, true);
		ByteBuffer buffer = ByteBuffer.wrap(frame, 0, frame.length - 2);

		assertNull(WsServerDecoder.decode(buffer, null));
	}

	@Test
	void decode_unmaskedFrame_throwsProtocolError() {
		byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.wrap(WsTestClient.buildFrame(0x1, payload, true, false));

		WsProtocolException e = assertThrows(WsProtocolException.class, () -> WsServerDecoder.decode(buffer, null));
		assertEquals(WsCloseCode.PROTOCOL_ERROR, e.getCloseCode());
	}

	@Test
	void decode_reservedBitsSet_throwsProtocolError() {
		// RSV1 置位，未协商扩展时不允许
		byte[] frame = WsTestClient.buildFrame(0x1, new byte[]{'a'}, true, true);
		frame[0] = (byte) (frame[0] | 0x40);
		ByteBuffer buffer = ByteBuffer.wrap(frame);

		WsProtocolException e = assertThrows(WsProtocolException.class, () -> WsServerDecoder.decode(buffer, null));
		assertEquals(WsCloseCode.PROTOCOL_ERROR, e.getCloseCode());
	}

	@Test
	void decode_unknownOpcode_throwsProtocolError() {
		// 0x3 是协议保留的操作码
		ByteBuffer buffer = ByteBuffer.wrap(WsTestClient.buildFrame(0x3, new byte[]{'a'}, true, true));

		WsProtocolException e = assertThrows(WsProtocolException.class, () -> WsServerDecoder.decode(buffer, null));
		assertEquals(WsCloseCode.PROTOCOL_ERROR, e.getCloseCode());
	}

	@Test
	void decode_fragmentedControlFrame_throwsProtocolError() {
		// 控制帧不允许分片，FIN 必须为 1
		ByteBuffer buffer = ByteBuffer.wrap(WsTestClient.buildFrame(0x9, new byte[]{'a'}, false, true));

		WsProtocolException e = assertThrows(WsProtocolException.class, () -> WsServerDecoder.decode(buffer, null));
		assertEquals(WsCloseCode.PROTOCOL_ERROR, e.getCloseCode());
	}

	@Test
	void decode_controlFramePayloadTooLong_throwsProtocolError() {
		// 控制帧负载不能超过 125 字节
		ByteBuffer buffer = ByteBuffer.wrap(WsTestClient.buildFrame(0x9, new byte[126], true, true));

		WsProtocolException e = assertThrows(WsProtocolException.class, () -> WsServerDecoder.decode(buffer, null));
		assertEquals(WsCloseCode.PROTOCOL_ERROR, e.getCloseCode());
	}

	@Test
	void decode_64BitLengthPayload_returnsPayload() throws Exception {
		byte[] payload = new byte[300];
		for (int i = 0; i < payload.length; i++) {
			payload[i] = (byte) i;
		}
		ByteBuffer buffer = ByteBuffer.wrap(WsTestClient.buildFrame(0x2, payload, true, true));

		WsRequest request = WsServerDecoder.decode(buffer, null);

		assertNotNull(request);
		assertEquals(Opcode.BINARY, request.getWsOpcode());
		assertArrayEquals(payload, request.getBody());
	}

	@Test
	void decode_64BitLengthWithHighestBitSet_throwsProtocolError() {
		// 64 位长度的最高位必须为 0
		byte[] frame = new byte[14];
		frame[0] = (byte) 0x82;
		frame[1] = (byte) 0xFF;
		// 8 字节长度：0x8000000000000000
		frame[2] = (byte) 0x80;
		ByteBuffer buffer = ByteBuffer.wrap(frame);

		WsProtocolException e = assertThrows(WsProtocolException.class, () -> WsServerDecoder.decode(buffer, null));
		assertEquals(WsCloseCode.PROTOCOL_ERROR, e.getCloseCode());
	}

	@Test
	void decode_payloadTooLarge_throwsProtocolError() {
		// 声明一个超大的长度，但不需要真的把数据发全
		byte[] frame = new byte[14];
		frame[0] = (byte) 0x82;
		frame[1] = (byte) 0xFF;
		// 8 字节长度：0x0000000000200001
		frame[7] = 0x20;
		frame[9] = 0x01;
		ByteBuffer buffer = ByteBuffer.wrap(frame);

		WsProtocolException e = assertThrows(WsProtocolException.class, () -> WsServerDecoder.decode(buffer, null));
		assertEquals(WsCloseCode.MESSAGE_TOO_BIG, e.getCloseCode());
	}

	@Test
	void decode_continuationFrame_returnsContinuationOpcode() throws Exception {
		ByteBuffer buffer = ByteBuffer.wrap(WsTestClient.buildFrame(0x0, new byte[]{'a'}, false, true));

		WsRequest request = WsServerDecoder.decode(buffer, null);

		assertNotNull(request);
		assertEquals(Opcode.CONTINUATION, request.getWsOpcode());
		assertEquals(false, request.isWsEof());
	}

	@Test
	void decode_maxBodyLengthBoundary_isAccepted() throws Exception {
		byte[] payload = new byte[WsPacket.MAX_BODY_LENGTH];
		ByteBuffer buffer = ByteBuffer.wrap(WsTestClient.buildFrame(0x2, payload, true, true));

		WsRequest request = WsServerDecoder.decode(buffer, null);

		assertNotNull(request);
		assertEquals(WsPacket.MAX_BODY_LENGTH, request.getBody().length);
	}
}
