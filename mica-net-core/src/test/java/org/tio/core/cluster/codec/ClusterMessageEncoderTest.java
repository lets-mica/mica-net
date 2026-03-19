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

package org.tio.core.cluster.codec;

import org.junit.jupiter.api.Test;
import org.tio.core.Node;
import org.tio.server.cluster.codec.ClusterMessageEncoder;
import org.tio.server.cluster.message.*;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class ClusterMessageEncoderTest {

	@Test
	void testEncodeDataMessageWithPayloadOnly() {
		byte[] payload = "hello".getBytes();
		ClusterDataMessage message = new ClusterDataMessage(payload);

		ByteBuffer buffer = ClusterMessageEncoder.INSTANCE.encode(message);

		assertNotNull(buffer);
		assertEquals(ClusterMessageType.DATA.getType(), buffer.get(0));
	}

	@Test
	void testEncodeDataMessageWithTimestamp() {
		long timestamp = 1234567890L;
		byte[] payload = "test".getBytes();
		ClusterDataMessage message = new ClusterDataMessage(timestamp, null, payload);

		ByteBuffer buffer = ClusterMessageEncoder.INSTANCE.encode(message);

		assertNotNull(buffer);
		assertEquals(ClusterMessageType.DATA.getType(), buffer.get(0));
		assertEquals(timestamp, buffer.getLong(1));
	}

	@Test
	void testEncodeSyncMessageWithPayloadOnly() {
		byte[] payload = "sync".getBytes();
		ClusterSyncMessage message = new ClusterSyncMessage(payload);

		ByteBuffer buffer = ClusterMessageEncoder.INSTANCE.encode(message);

		assertNotNull(buffer);
		assertEquals(ClusterMessageType.SYNC.getType(), buffer.get(0));
		assertTrue(message.getMessageId() > 0);
	}

	@Test
	void testEncodeSyncMessageFromDataMessage() {
		byte[] payload = "convert".getBytes();
		ClusterDataMessage dataMessage = new ClusterDataMessage(System.currentTimeMillis(), null, payload);
		ClusterSyncMessage syncMessage = new ClusterSyncMessage(dataMessage);

		ByteBuffer buffer = ClusterMessageEncoder.INSTANCE.encode(syncMessage);

		assertNotNull(buffer);
		assertEquals(ClusterMessageType.SYNC.getType(), buffer.get(0));
		assertEquals(payload.length, syncMessage.getPayload().length);
	}

	@Test
	void testEncodeSyncMessageWithTimestampAndHeaders() {
		long messageId = 12345L;
		long timestamp = 9876543210L;
		byte[] payload = "full".getBytes();
		ClusterSyncMessage message = new ClusterSyncMessage(messageId, timestamp, null, payload);

		ByteBuffer buffer = ClusterMessageEncoder.INSTANCE.encode(message);

		assertNotNull(buffer);
		assertEquals(ClusterMessageType.SYNC.getType(), buffer.get(0));
		assertEquals(messageId, message.getMessageId());
	}

	@Test
	void testEncodePingMessage() {
		ByteBuffer buffer = ClusterMessageEncoder.INSTANCE.encode(ClusterPingMessage.INSTANCE);

		assertNotNull(buffer);
		assertEquals(ClusterMessageType.PING.getType(), buffer.get(0));
	}

	@Test
	void testEncodePongMessage() {
		ByteBuffer buffer = ClusterMessageEncoder.INSTANCE.encode(ClusterPongMessage.INSTANCE);

		assertNotNull(buffer);
		assertEquals(ClusterMessageType.PONG.getType(), buffer.get(0));
	}

	@Test
	void testEncodeJoinMessage() {
		Node node = new Node("127.0.0.1", 3001);
		ClusterJoinMessage message = new ClusterJoinMessage(node);

		ByteBuffer buffer = ClusterMessageEncoder.INSTANCE.encode(message);

		assertNotNull(buffer);
		assertEquals(ClusterMessageType.JOIN.getType(), buffer.get(0));
	}

	@Test
	void testEncodeSyncAckMessage() {
		long messageId = 99999L;
		ClusterSyncAckMessage message = new ClusterSyncAckMessage(messageId);

		ByteBuffer buffer = ClusterMessageEncoder.INSTANCE.encode(message);

		assertNotNull(buffer);
		assertEquals(ClusterMessageType.SYNC_ACK.getType(), buffer.get(0));
	}
}
