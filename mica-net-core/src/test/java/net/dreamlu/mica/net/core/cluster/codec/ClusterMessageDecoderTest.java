/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & www.dreamlu.net).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dreamlu.mica.net.core.cluster.codec;

import net.dreamlu.mica.net.core.Node;
import net.dreamlu.mica.net.core.exception.TioDecodeException;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.server.cluster.codec.ClusterMessageDecoder;
import net.dreamlu.mica.net.server.cluster.codec.ClusterMessageEncoder;
import net.dreamlu.mica.net.server.cluster.message.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class ClusterMessageDecoderTest {

	private ClusterMessageDecoder decoder;

	@BeforeEach
	void setUp() {
		decoder = new ClusterMessageDecoder();
	}

	@Test
	void testDecodePingMessage() throws TioDecodeException {
		ByteBuffer buffer = ByteBuffer.allocate(1);
		buffer.put(ClusterMessageType.PING.getType());
		buffer.flip();

		Packet packet = decoder.decode(null, buffer, buffer.remaining());

		assertNotNull(packet);
		assertTrue(packet instanceof ClusterPingMessage);
	}

	@Test
	void testDecodePongMessage() throws TioDecodeException {
		ByteBuffer buffer = ByteBuffer.allocate(1);
		buffer.put(ClusterMessageType.PONG.getType());
		buffer.flip();

		Packet packet = decoder.decode(null, buffer, buffer.remaining());

		assertNotNull(packet);
		assertTrue(packet instanceof ClusterPongMessage);
	}

	@Test
	void testDecodeDataMessageWithPayloadOnly() throws TioDecodeException {
		byte[] payload = "hello".getBytes();
		ClusterDataMessage original = new ClusterDataMessage(payload);
		ByteBuffer encoded = ClusterMessageEncoder.INSTANCE.encode(original);
		encoded.flip();

		Packet packet = decoder.decode(null, encoded, encoded.remaining());

		assertNotNull(packet);
		assertTrue(packet instanceof ClusterDataMessage);
		ClusterDataMessage decoded = (ClusterDataMessage) packet;
		assertArrayEquals(payload, decoded.getPayload());
		assertNotNull(decoded.getTimestamp());
		assertNotNull(decoded.getHeaders());
	}

	@Test
	void testDecodeDataMessageWithTimestamp() throws TioDecodeException {
		long timestamp = 1234567890L;
		byte[] payload = "test".getBytes();
		ClusterDataMessage original = new ClusterDataMessage(timestamp, null, payload);
		ByteBuffer encoded = ClusterMessageEncoder.INSTANCE.encode(original);
		encoded.flip();

		Packet packet = decoder.decode(null, encoded, encoded.remaining());

		assertNotNull(packet);
		assertTrue(packet instanceof ClusterDataMessage);
		ClusterDataMessage decoded = (ClusterDataMessage) packet;
		assertArrayEquals(payload, decoded.getPayload());
		assertEquals(timestamp, decoded.getTimestamp());
		assertNotNull(decoded.getHeaders());
	}

	@Test
	void testDecodeSyncMessageWithPayloadOnly() throws TioDecodeException {
		byte[] payload = "sync".getBytes();
		ClusterSyncMessage original = new ClusterSyncMessage(payload);
		ByteBuffer encoded = ClusterMessageEncoder.INSTANCE.encode(original);
		encoded.flip();

		Packet packet = decoder.decode(null, encoded, encoded.remaining());

		assertNotNull(packet);
		assertTrue(packet instanceof ClusterSyncMessage);
		ClusterSyncMessage decoded = (ClusterSyncMessage) packet;
		assertArrayEquals(payload, decoded.getPayload());
		assertTrue(decoded.getMessageId() > 0);
		assertNotNull(decoded.getTimestamp());
		assertNotNull(decoded.getHeaders());
	}

	@Test
	void testDecodeSyncMessageFromDataMessage() throws TioDecodeException {
		byte[] payload = "converted".getBytes();
		ClusterDataMessage dataMessage = new ClusterDataMessage(System.currentTimeMillis(), null, payload);
		ClusterSyncMessage syncMessage = new ClusterSyncMessage(dataMessage);
		ByteBuffer encoded = ClusterMessageEncoder.INSTANCE.encode(syncMessage);
		encoded.flip();

		Packet packet = decoder.decode(null, encoded, encoded.remaining());

		assertNotNull(packet);
		assertTrue(packet instanceof ClusterSyncMessage);
		ClusterSyncMessage decoded = (ClusterSyncMessage) packet;
		assertArrayEquals(payload, decoded.getPayload());
	}

	@Test
	void testDecodeSyncAckMessage() throws TioDecodeException {
		long messageId = 99999L;
		ClusterSyncAckMessage original = new ClusterSyncAckMessage(messageId);
		ByteBuffer encoded = ClusterMessageEncoder.INSTANCE.encode(original);
		encoded.flip();

		Packet packet = decoder.decode(null, encoded, encoded.remaining());

		assertNotNull(packet);
		assertTrue(packet instanceof ClusterSyncAckMessage);
		ClusterSyncAckMessage decoded = (ClusterSyncAckMessage) packet;
		assertEquals(messageId, decoded.getMessageId());
	}

	@Test
	void testDecodeJoinMessage() throws TioDecodeException {
		Node node = new Node("127.0.0.1", 3001);
		ClusterJoinMessage original = new ClusterJoinMessage(node);
		ByteBuffer encoded = ClusterMessageEncoder.INSTANCE.encode(original);
		encoded.flip();

		Packet packet = decoder.decode(null, encoded, encoded.remaining());

		assertNotNull(packet);
		assertTrue(packet instanceof ClusterJoinMessage);
		ClusterJoinMessage decoded = (ClusterJoinMessage) packet;
		assertEquals("127.0.0.1", decoded.getJoinMember().getIp().trim());
		assertEquals(3001, decoded.getJoinMember().getPort());
	}

	@Test
	void testDecodeInsufficientData() throws TioDecodeException {
		ByteBuffer buffer = ByteBuffer.allocate(0);

		Packet packet = decoder.decode(null, buffer, buffer.remaining());

		assertNull(packet);
	}

	@Test
	void testDecodeUnknownMessageType() throws TioDecodeException {
		ByteBuffer buffer = ByteBuffer.allocate(1);
		buffer.put((byte) 999);
		buffer.flip();

		assertThrows(TioDecodeException.class, () -> decoder.decode(null, buffer, buffer.remaining()));
	}
}
