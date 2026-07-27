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

import javax.net.ssl.SSLSession;
import java.nio.ByteBuffer;

/**
 * SSLEngine buffer allocation and inbound TLS fragment accumulation.
 *
 * @author L.cm
 */
final class SslBuffers {
	private final SSLSession session;
	private ByteBuffer inboundNetwork;

	SslBuffers(SSLSession session) {
		this.session = session;
		this.inboundNetwork = ByteBuffer.allocate(session.getPacketBufferSize());
	}

	void appendEncrypted(ByteBuffer encryptedData) {
		ensureInboundCapacity(encryptedData.remaining());
		inboundNetwork.put(encryptedData);
	}

	ByteBuffer beginUnwrap() {
		inboundNetwork.flip();
		return inboundNetwork;
	}

	void endUnwrap(boolean underflow) {
		inboundNetwork.compact();
		if (underflow && !inboundNetwork.hasRemaining()) {
			inboundNetwork = grow(inboundNetwork, inboundNetwork.capacity() + 1);
		}
	}

	ByteBuffer applicationBuffer() {
		return ByteBuffer.allocate(session.getApplicationBufferSize());
	}

	ByteBuffer packetBuffer() {
		return ByteBuffer.allocate(session.getPacketBufferSize());
	}

	ByteBuffer growApplicationBuffer(ByteBuffer buffer) {
		return grow(buffer, Math.max(session.getApplicationBufferSize(), buffer.capacity() + 1));
	}

	ByteBuffer growPacketBuffer(ByteBuffer buffer) {
		return grow(buffer, Math.max(session.getPacketBufferSize(), buffer.capacity() + 1));
	}

	static ByteBuffer toReadBuffer(ByteBuffer buffer) {
		buffer.flip();
		return buffer;
	}

	private void ensureInboundCapacity(int additionalBytes) {
		if (inboundNetwork.remaining() >= additionalBytes) {
			return;
		}
		inboundNetwork = grow(inboundNetwork, inboundNetwork.position() + additionalBytes);
	}

	private static ByteBuffer grow(ByteBuffer original, int minimumCapacity) {
		int newCapacity = Math.max(minimumCapacity, original.capacity() << 1);
		ByteBuffer expanded = ByteBuffer.allocate(newCapacity);
		original.flip();
		expanded.put(original);
		return expanded;
	}
}
