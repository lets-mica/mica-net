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

package net.dreamlu.mica.net.core.ssl;

import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import net.dreamlu.mica.net.utils.buffer.ByteBufferUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SslBuffersTest {

	@Test
	void shouldGrowAndPreserveFragmentedInboundData() throws Exception {
		SSLSession session = SSLContext.getDefault().createSSLEngine().getSession();
		SslBuffers buffers = new SslBuffers(session);
		byte[] first = new byte[session.getPacketBufferSize() + 128];
		for (int i = 0; i < first.length; i++) {
			first[i] = (byte) i;
		}

		buffers.appendEncrypted(ByteBuffer.wrap(first));
		ByteBuffer inbound = buffers.beginUnwrap();
		Assertions.assertEquals(first.length, inbound.remaining());
		byte[] consumed = new byte[100];
		inbound.get(consumed);
		Assertions.assertArrayEquals(Arrays.copyOf(first, consumed.length), consumed);
		buffers.endUnwrap(true);

		byte[] second = new byte[]{11, 12, 13};
		buffers.appendEncrypted(ByteBuffer.wrap(second));
		inbound = buffers.beginUnwrap();
		byte[] remaining = new byte[inbound.remaining()];
		inbound.get(remaining);
		byte[] expected = new byte[first.length - consumed.length + second.length];
		System.arraycopy(first, consumed.length, expected, 0, first.length - consumed.length);
		System.arraycopy(second, 0, expected, first.length - consumed.length, second.length);
		Assertions.assertArrayEquals(expected, remaining);
		buffers.endUnwrap(false);
	}

	@Test
	void shouldGrowApplicationAndPacketBuffersWithoutLosingData() throws Exception {
		SSLSession session = SSLContext.getDefault().createSSLEngine().getSession();
		SslBuffers buffers = new SslBuffers(session);
		ByteBuffer application = buffers.applicationBuffer();
		application.put(new byte[]{1, 2, 3});
		int applicationCapacity = application.capacity();
		application = buffers.growApplicationBuffer(application);
		Assertions.assertTrue(application.capacity() > applicationCapacity);
		Assertions.assertArrayEquals(new byte[]{1, 2, 3}, readWrittenData(application));

		ByteBuffer packet = buffers.packetBuffer();
		packet.put(new byte[]{4, 5, 6});
		int packetCapacity = packet.capacity();
		packet = buffers.growPacketBuffer(packet);
		Assertions.assertTrue(packet.capacity() > packetCapacity);
		Assertions.assertArrayEquals(new byte[]{4, 5, 6}, readWrittenData(packet));
	}

	@Test
	void shouldCombineOnlyRemainingBytes() {
		ByteBuffer first = ByteBuffer.wrap(new byte[]{0, 1, 2});
		first.position(1);
		ByteBuffer combined = ByteBufferUtil.combine(Arrays.asList(first, ByteBuffer.wrap(new byte[]{3, 4})));
		byte[] actual = new byte[combined.remaining()];
		combined.get(actual);
		Assertions.assertArrayEquals(new byte[]{1, 2, 3, 4}, actual);
	}

	private static byte[] readWrittenData(ByteBuffer buffer) {
		buffer.flip();
		byte[] data = new byte[buffer.remaining()];
		buffer.get(data);
		return data;
	}
}
