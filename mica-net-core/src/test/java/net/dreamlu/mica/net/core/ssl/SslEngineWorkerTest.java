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
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.dreamlu.mica.net.client.ClientChannelContext;
import net.dreamlu.mica.net.client.DefaultTioClientListener;
import net.dreamlu.mica.net.client.TioClientConfig;
import net.dreamlu.mica.net.core.tcp.FixedLengthCodec;
import net.dreamlu.mica.net.core.tcp.TestTioClientHandler;
import net.dreamlu.mica.net.core.tcp.TestTioServerHandler;
import net.dreamlu.mica.net.server.DefaultTioServerListener;
import net.dreamlu.mica.net.server.ServerChannelContext;
import net.dreamlu.mica.net.server.TioServerConfig;

class SslEngineWorkerTest {
	private static final String KEY_STORE = "classpath:test.jks";
	private static final String PASSWORD = "501937";

	@Test
	void shouldHandshakeAndTransferMultipleTlsRecords() throws Exception {
		Queue<ByteBuffer> clientToServer = new ArrayDeque<>();
		Queue<ByteBuffer> serverToClient = new ArrayDeque<>();
		Queue<ByteBuffer> clientPlainData = new ArrayDeque<>();
		Queue<ByteBuffer> serverPlainData = new ArrayDeque<>();
		AtomicBoolean clientHandshakeCompleted = new AtomicBoolean();
		AtomicBoolean serverHandshakeCompleted = new AtomicBoolean();
		AtomicBoolean clientSessionClosed = new AtomicBoolean();

		SslConfig clientSslConfig = SslConfig.forClient(KEY_STORE, PASSWORD);
		SslConfig serverSslConfig = SslConfig.forServer(KEY_STORE, PASSWORD);
		SslEngineWorker client = new SslEngineWorker(newClientContext(), clientSslConfig.getSslContext(), true, clientSslConfig,
			clientToServer::add, clientPlainData::add, () -> clientHandshakeCompleted.set(true), () -> clientSessionClosed.set(true));
		SslEngineWorker server = new SslEngineWorker(newServerContext(), serverSslConfig.getSslContext(), false, serverSslConfig,
			serverToClient::add, serverPlainData::add, () -> serverHandshakeCompleted.set(true), () -> { });

		server.beginHandshake();
		client.beginHandshake();
		pump(client, server, clientToServer, serverToClient);

		Assertions.assertTrue(clientHandshakeCompleted.get());
		Assertions.assertTrue(serverHandshakeCompleted.get());
		Assertions.assertTrue(client.isHandshakeCompleted());
		Assertions.assertTrue(server.isHandshakeCompleted());

		byte[] expected = new byte[64 * 1024];
		for (int i = 0; i < expected.length; i++) {
			expected[i] = (byte) i;
		}
		clientToServer.add(client.encrypt(ByteBuffer.wrap(expected)));
		pump(client, server, clientToServer, serverToClient);

		ByteBuffer actualBuffer = combine(serverPlainData);
		byte[] actual = new byte[actualBuffer.remaining()];
		actualBuffer.get(actual);
		Assertions.assertArrayEquals(expected, actual);

		byte[] response = "server response".getBytes(StandardCharsets.UTF_8);
		serverToClient.add(server.encrypt(ByteBuffer.wrap(response)));
		pump(client, server, clientToServer, serverToClient);
		ByteBuffer responseBuffer = combine(clientPlainData);
		byte[] actualResponse = new byte[responseBuffer.remaining()];
		responseBuffer.get(actualResponse);
		Assertions.assertArrayEquals(response, actualResponse);

		server.close();
		pump(client, server, clientToServer, serverToClient);
		Assertions.assertTrue(clientSessionClosed.get());
	}

	@Test
	void shouldIgnoreDuplicateHandshakeStart() throws Exception {
		Queue<ByteBuffer> clientToServer = new ArrayDeque<>();
		Queue<ByteBuffer> serverToClient = new ArrayDeque<>();
		AtomicBoolean clientCompleted = new AtomicBoolean();
		AtomicBoolean serverCompleted = new AtomicBoolean();
		SslConfig clientConfig = SslConfig.forClient(KEY_STORE, PASSWORD);
		SslConfig serverConfig = SslConfig.forServer(KEY_STORE, PASSWORD);
		SslEngineWorker client = new SslEngineWorker(newClientContext(), clientConfig.getSslContext(), true, clientConfig,
			clientToServer::add, data -> { }, () -> clientCompleted.set(true), () -> { });
		SslEngineWorker server = new SslEngineWorker(newServerContext(), serverConfig.getSslContext(), false, serverConfig,
			serverToClient::add, data -> { }, () -> serverCompleted.set(true), () -> { });

		server.beginHandshake();
		client.beginHandshake();
		int initialClientRecords = clientToServer.size();
		client.beginHandshake();
		Assertions.assertEquals(initialClientRecords, clientToServer.size());
		pump(client, server, clientToServer, serverToClient);

		Assertions.assertTrue(clientCompleted.get());
		Assertions.assertTrue(serverCompleted.get());
	}

	@Test
	void shouldRejectClientWithoutCertificateWhenRequired() throws Exception {
		Queue<ByteBuffer> clientToServer = new ArrayDeque<>();
		Queue<ByteBuffer> serverToClient = new ArrayDeque<>();
		SslConfig clientConfig = SslConfig.forClient(KEY_STORE, PASSWORD);
		SslConfig serverConfig = SslConfig.forServer(KEY_STORE, PASSWORD, ClientAuth.REQUIRE);
		SslEngineWorker client = new SslEngineWorker(newClientContext(), clientConfig.getSslContext(), true, clientConfig,
			clientToServer::add, data -> { }, () -> { }, () -> { });
		SslEngineWorker server = new SslEngineWorker(newServerContext(), serverConfig.getSslContext(), false, serverConfig,
			serverToClient::add, data -> { }, () -> { }, () -> { });

		server.beginHandshake();
		client.beginHandshake();
		Assertions.assertThrows(SSLException.class, () -> pump(client, server, clientToServer, serverToClient));
		Assertions.assertFalse(server.isHandshakeCompleted());
	}

	@Test
	void shouldAllowMissingClientCertificateWhenOptional() throws Exception {
		Queue<ByteBuffer> clientToServer = new ArrayDeque<>();
		Queue<ByteBuffer> serverToClient = new ArrayDeque<>();
		AtomicBoolean clientCompleted = new AtomicBoolean();
		AtomicBoolean serverCompleted = new AtomicBoolean();
		SslConfig clientConfig = SslConfig.forClient(KEY_STORE, PASSWORD);
		SslConfig serverConfig = SslConfig.forServer(KEY_STORE, PASSWORD, ClientAuth.OPTIONAL);
		SslEngineWorker client = new SslEngineWorker(newClientContext(), clientConfig.getSslContext(), true, clientConfig,
			clientToServer::add, data -> { }, () -> clientCompleted.set(true), () -> { });
		SslEngineWorker server = new SslEngineWorker(newServerContext(), serverConfig.getSslContext(), false, serverConfig,
			serverToClient::add, data -> { }, () -> serverCompleted.set(true), () -> { });

		server.beginHandshake();
		client.beginHandshake();
		pump(client, server, clientToServer, serverToClient);
		Assertions.assertTrue(clientCompleted.get());
		Assertions.assertTrue(serverCompleted.get());
	}

	@Test
	void shouldRejectPlainTextOnTlsConnection() throws Exception {
		SslConfig serverConfig = SslConfig.forServer(KEY_STORE, PASSWORD);
		SslEngineWorker server = new SslEngineWorker(newServerContext(), serverConfig.getSslContext(), false, serverConfig,
			data -> { }, data -> { }, () -> { }, () -> { });
		server.beginHandshake();

		ByteBuffer plainHttp = ByteBuffer.wrap("GET / HTTP/1.1\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
		Assertions.assertThrows(SSLException.class, () -> server.decrypt(plainHttp));
	}

	@Test
	void shouldApplyEngineCustomizer() {
		AtomicBoolean customized = new AtomicBoolean();
		SslConfig clientConfig = SslConfig.forClient(KEY_STORE, PASSWORD);
		clientConfig.setSslEngineCustomizer(engine -> customized.set(true));

		new SslEngineWorker(newClientContext(), clientConfig.getSslContext(), true, clientConfig,
			data -> { }, data -> { }, () -> { }, () -> { });

		Assertions.assertTrue(customized.get());
	}

	private static void pump(SslEngineWorker client, SslEngineWorker server,
						 Queue<ByteBuffer> clientToServer, Queue<ByteBuffer> serverToClient) throws SSLException {
		for (int i = 0; i < 100 && (!clientToServer.isEmpty() || !serverToClient.isEmpty()); i++) {
			ByteBuffer encryptedData;
			while ((encryptedData = clientToServer.poll()) != null) {
				deliverFragments(server, encryptedData);
			}
			while ((encryptedData = serverToClient.poll()) != null) {
				deliverFragments(client, encryptedData);
			}
		}
		Assertions.assertTrue(clientToServer.isEmpty(), "client TLS output was not drained");
		Assertions.assertTrue(serverToClient.isEmpty(), "server TLS output was not drained");
	}

	private static void deliverFragments(SslEngineWorker receiver, ByteBuffer encryptedData) throws SSLException {
		while (encryptedData.hasRemaining()) {
			int fragmentLength = Math.min(37, encryptedData.remaining());
			ByteBuffer fragment = encryptedData.slice();
			fragment.limit(fragmentLength);
			receiver.decrypt(fragment);
			encryptedData.position(encryptedData.position() + fragmentLength);
		}
	}

	private static ByteBuffer combine(Queue<ByteBuffer> buffers) {
		int length = 0;
		for (ByteBuffer buffer : buffers) {
			length += buffer.remaining();
		}
		ByteBuffer combined = ByteBuffer.allocate(length);
		ByteBuffer buffer;
		while ((buffer = buffers.poll()) != null) {
			combined.put(buffer);
		}
		combined.flip();
		return combined;
	}

	private static ClientChannelContext newClientContext() {
		FixedLengthCodec codec = new FixedLengthCodec(8);
		TioClientConfig config = new TioClientConfig(new TestTioClientHandler(codec), new DefaultTioClientListener());
		return new ClientChannelContext(config);
	}

	private static ServerChannelContext newServerContext() {
		FixedLengthCodec codec = new FixedLengthCodec(8);
		TioServerConfig config = new TioServerConfig(new TestTioServerHandler(codec), new DefaultTioServerListener());
		return new ServerChannelContext(config, "ssl-engine-worker-test");
	}
}
