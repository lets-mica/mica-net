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

package net.dreamlu.mica.net.core.tcp;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.server.DefaultTioServerListener;
import net.dreamlu.mica.net.server.ServerChannelContext;
import net.dreamlu.mica.net.server.TioServerConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

class TcpSendRunnableTest {

	@Test
	void shouldSendSinglePacketWithoutReadingQueueSize() {
		NoSizeQueue queue = new NoSizeQueue();
		Packet packet = packet((byte) 1);
		queue.add(packet);
		TestTcpSendRunnable runnable = newRunnable(queue);

		runnable.runTask();

		Assertions.assertSame(packet, runnable.singlePacket);
		Assertions.assertNull(runnable.batchPackets);
		Assertions.assertTrue(queue.isEmpty());
	}

	@Test
	void shouldBatchPacketsWithoutReadingQueueSize() {
		NoSizeQueue queue = new NoSizeQueue();
		Packet first = packet((byte) 1);
		Packet second = packet((byte) 2);
		Packet third = packet((byte) 3);
		queue.add(first);
		queue.add(second);
		queue.add(third);
		TestTcpSendRunnable runnable = newRunnable(queue);

		runnable.runTask();

		Assertions.assertNull(runnable.singlePacket);
		Assertions.assertEquals(3, runnable.batchBuffers.length);
		Assertions.assertEquals(Arrays.asList(first, second, third), runnable.batchPackets);
		Assertions.assertTrue(queue.isEmpty());
	}

	@Test
	void shouldKeepPacketsBeyondBatchLimitInQueue() {
		NoSizeQueue queue = new NoSizeQueue();
		for (int i = 0; i <= TestTcpSendRunnable.maxBatchSize(); i++) {
			queue.add(packet((byte) i));
		}
		TestTcpSendRunnable runnable = newRunnable(queue);

		runnable.runTask();

		Assertions.assertEquals(TestTcpSendRunnable.maxBatchSize(), runnable.batchBuffers.length);
		Assertions.assertEquals(1, queue.sizeWithoutFailure());
		Packet remainingPacket = queue.peek();

		runnable.onWriteCompleted();

		Assertions.assertSame(remainingPacket, runnable.singlePacket);
		Assertions.assertTrue(queue.isEmpty());
	}

	@Test
	void shouldKeepPacketsBeyondByteLimitInQueue() {
		NoSizeQueue queue = new NoSizeQueue();
		queue.add(packet(TestTcpSendRunnable.maxBatchBytes()));
		Packet remainingPacket = packet((byte) 2);
		queue.add(remainingPacket);
		TestTcpSendRunnable runnable = newRunnable(queue);

		runnable.runTask();

		Assertions.assertEquals(1, runnable.batchBuffers.length);
		Assertions.assertSame(remainingPacket, queue.peek());
	}

	@Test
	void shouldNotMixPlainAndEncryptedPacketsInSslBatch() {
		NoSizeQueue queue = new NoSizeQueue();
		Packet plainPacket = packet((byte) 1);
		Packet encryptedPacket = encryptedPacket((byte) 2);
		queue.add(plainPacket);
		queue.add(encryptedPacket);
		TestTcpSendRunnable runnable = newRunnable(queue);

		runnable.captureBatch(true);

		Assertions.assertEquals(Arrays.asList(plainPacket), runnable.batchPackets);
		Assertions.assertTrue(runnable.batchNeedSslEncrypted);
		Assertions.assertSame(encryptedPacket, queue.peek());

		runnable.captureBatch(true);

		Assertions.assertEquals(Arrays.asList(encryptedPacket), runnable.batchPackets);
		Assertions.assertFalse(runnable.batchNeedSslEncrypted);
		Assertions.assertTrue(queue.isEmpty());
	}

	@Test
	void shouldNotMixEncryptedAndPlainPacketsInSslBatch() {
		NoSizeQueue queue = new NoSizeQueue();
		Packet encryptedPacket = encryptedPacket((byte) 1);
		Packet plainPacket = packet((byte) 2);
		queue.add(encryptedPacket);
		queue.add(plainPacket);
		TestTcpSendRunnable runnable = newRunnable(queue);

		runnable.captureBatch(true);

		Assertions.assertEquals(Arrays.asList(encryptedPacket), runnable.batchPackets);
		Assertions.assertFalse(runnable.batchNeedSslEncrypted);
		Assertions.assertSame(plainPacket, queue.peek());
	}

	private static TestTcpSendRunnable newRunnable(NoSizeQueue queue) {
		FixedLengthCodec codec = new FixedLengthCodec(1);
		TioServerConfig config = new TioServerConfig(
			new TestTioServerHandler(codec), new DefaultTioServerListener()
		);
		ChannelContext channelContext = new ServerChannelContext(config, "tcp-send-runnable-test");
		return new TestTcpSendRunnable(channelContext, queue);
	}

	private static Packet packet(byte value) {
		return packet(new byte[]{value});
	}

	private static Packet packet(int size) {
		return packet(new byte[size]);
	}

	private static Packet packet(byte[] bytes) {
		Packet packet = new Packet();
		packet.setPreEncodedByteBuffer(ByteBuffer.wrap(bytes));
		return packet;
	}

	private static Packet encryptedPacket(byte value) {
		Packet packet = packet(value);
		packet.setSslEncrypted(true);
		return packet;
	}

	private static final class NoSizeQueue extends ConcurrentLinkedQueue<Packet> {
		@Override
		public int size() {
			throw new AssertionError("发送热路径不应遍历队列计算 size");
		}

		private int sizeWithoutFailure() {
			int size = 0;
			for (Packet ignored : this) {
				size++;
			}
			return size;
		}
	}

	private static final class TestTcpSendRunnable extends TcpSendRunnable {
		private Packet singlePacket;
		private ByteBuffer[] batchBuffers;
		private List<Packet> batchPackets;
		private boolean batchNeedSslEncrypted;

		private TestTcpSendRunnable(ChannelContext channelContext, NoSizeQueue queue) {
			super(channelContext, Runnable::run, queue);
		}

		@Override
		public boolean sendPacket(Packet packet, boolean isSsl) {
			this.singlePacket = packet;
			return true;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void sendByteBuffers(ByteBuffer[] byteBuffers, Object packets) {
			this.batchBuffers = byteBuffers;
			this.batchPackets = (List<Packet>) packets;
		}

		private void captureBatch(boolean isSsl) {
			capture(batchEncode(getMsgQueue().poll(), isSsl));
		}

		private void capture(BatchEncodeResult result) {
			this.batchBuffers = result.byteBuffers;
			this.batchPackets = result.packets;
			this.batchNeedSslEncrypted = result.needSslEncrypted;
		}

		private static int maxBatchSize() {
			return MAX_BATCH_SIZE;
		}

		private static int maxBatchBytes() {
			return MAX_CAPACITY_MAX;
		}
	}
}
