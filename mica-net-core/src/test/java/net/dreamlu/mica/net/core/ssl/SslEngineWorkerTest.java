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
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
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
	private static final String TLS_1_2 = "TLSv1.2";
	private static final String TLS_1_3 = "TLSv1.3";

	@Test
	void shouldHandshakeAndTransferWithTls12UsingSingleByteFragments() throws Exception {
		assertProtocolHandshakeAndTransfer(TLS_1_2, 1);
	}

	@Test
	void shouldHandshakeAndTransferWithTls13() throws Exception {
		assertProtocolHandshakeAndTransfer(TLS_1_3, 16 * 1024 + 7);
	}

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

	@Test
	void shouldConsumeCloseNotifyFromEitherPeer() throws Exception {
		assertCloseNotify(true);
		assertCloseNotify(false);
	}

	private static void assertProtocolHandshakeAndTransfer(String protocol, int fragmentLength) throws Exception {
		Queue<ByteBuffer> clientToServer = new ArrayDeque<>();
		Queue<ByteBuffer> serverToClient = new ArrayDeque<>();
		Queue<ByteBuffer> clientPlainData = new ArrayDeque<>();
		Queue<ByteBuffer> serverPlainData = new ArrayDeque<>();
		AtomicReference<SSLEngine> clientEngine = new AtomicReference<>();
		AtomicReference<SSLEngine> serverEngine = new AtomicReference<>();

		SslConfig clientConfig = SslConfig.forClient(KEY_STORE, PASSWORD);
		SslConfig serverConfig = SslConfig.forServer(KEY_STORE, PASSWORD);
		Assumptions.assumeTrue(supportsProtocol(clientConfig.getSslContext(), protocol));
		Assumptions.assumeTrue(supportsProtocol(serverConfig.getSslContext(), protocol));
		clientConfig.setSslEngineCustomizer(engine -> {
			engine.setEnabledProtocols(new String[]{protocol});
			clientEngine.set(engine);
		});
		serverConfig.setSslEngineCustomizer(engine -> {
			engine.setEnabledProtocols(new String[]{protocol});
			serverEngine.set(engine);
		});

		SslEngineWorker client = new SslEngineWorker(newClientContext(), clientConfig.getSslContext(), true, clientConfig,
			clientToServer::add, clientPlainData::add, () -> { }, () -> { });
		SslEngineWorker server = new SslEngineWorker(newServerContext(), serverConfig.getSslContext(), false, serverConfig,
			serverToClient::add, serverPlainData::add, () -> { }, () -> { });

		server.beginHandshake();
		client.beginHandshake();
		pump(client, server, clientToServer, serverToClient, fragmentLength);

		Assertions.assertTrue(client.isHandshakeCompleted());
		Assertions.assertTrue(server.isHandshakeCompleted());
		Assertions.assertEquals(protocol, clientEngine.get().getSession().getProtocol());
		Assertions.assertEquals(protocol, serverEngine.get().getSession().getProtocol());

		byte[] request = testData(48 * 1024 + 13);
		clientToServer.add(client.encrypt(ByteBuffer.wrap(request)));
		pump(client, server, clientToServer, serverToClient, fragmentLength);
		assertPlainData(request, serverPlainData);

		byte[] response = testData(32 * 1024 + 29);
		serverToClient.add(server.encrypt(ByteBuffer.wrap(response)));
		pump(client, server, clientToServer, serverToClient, fragmentLength);
		assertPlainData(response, clientPlainData);
	}

	private static void assertCloseNotify(boolean closeServer) throws Exception {
		Queue<ByteBuffer> clientToServer = new ArrayDeque<>();
		Queue<ByteBuffer> serverToClient = new ArrayDeque<>();
		AtomicBoolean clientClosed = new AtomicBoolean();
		AtomicBoolean serverClosed = new AtomicBoolean();
		SslConfig clientConfig = SslConfig.forClient(KEY_STORE, PASSWORD);
		SslConfig serverConfig = SslConfig.forServer(KEY_STORE, PASSWORD);
		SslEngineWorker client = new SslEngineWorker(newClientContext(), clientConfig.getSslContext(), true, clientConfig,
			clientToServer::add, data -> { }, () -> { }, () -> clientClosed.set(true));
		SslEngineWorker server = new SslEngineWorker(newServerContext(), serverConfig.getSslContext(), false, serverConfig,
			serverToClient::add, data -> { }, () -> { }, () -> serverClosed.set(true));

		server.beginHandshake();
		client.beginHandshake();
		pump(client, server, clientToServer, serverToClient);
		if (closeServer) {
			server.close();
		} else {
			client.close();
		}
		pump(client, server, clientToServer, serverToClient);

		Assertions.assertEquals(closeServer, clientClosed.get());
		Assertions.assertEquals(!closeServer, serverClosed.get());
	}

	private static boolean supportsProtocol(SSLContext sslContext, String protocol) {
		return Arrays.asList(sslContext.getSupportedSSLParameters().getProtocols()).contains(protocol);
	}

	private static byte[] testData(int length) {
		byte[] data = new byte[length];
		for (int i = 0; i < length; i++) {
			data[i] = (byte) (i * 31);
		}
		return data;
	}

	private static void assertPlainData(byte[] expected, Queue<ByteBuffer> plainData) {
		ByteBuffer actualBuffer = combine(plainData);
		byte[] actual = new byte[actualBuffer.remaining()];
		actualBuffer.get(actual);
		Assertions.assertArrayEquals(expected, actual);
	}

	private static void pump(SslEngineWorker client, SslEngineWorker server,
						 Queue<ByteBuffer> clientToServer, Queue<ByteBuffer> serverToClient) throws SSLException {
		pump(client, server, clientToServer, serverToClient, 37);
	}

	private static void pump(SslEngineWorker client, SslEngineWorker server,
						 Queue<ByteBuffer> clientToServer, Queue<ByteBuffer> serverToClient,
						 int fragmentLength) throws SSLException {
		for (int i = 0; i < 100 && (!clientToServer.isEmpty() || !serverToClient.isEmpty()); i++) {
			ByteBuffer encryptedData;
			while ((encryptedData = clientToServer.poll()) != null) {
				deliverFragments(server, encryptedData, fragmentLength);
			}
			while ((encryptedData = serverToClient.poll()) != null) {
				deliverFragments(client, encryptedData, fragmentLength);
			}
		}
		Assertions.assertTrue(clientToServer.isEmpty(), "client TLS output was not drained");
		Assertions.assertTrue(serverToClient.isEmpty(), "server TLS output was not drained");
	}

	private static void deliverFragments(SslEngineWorker receiver, ByteBuffer encryptedData,
									 int fragmentLength) throws SSLException {
		while (encryptedData.hasRemaining()) {
			int currentFragmentLength = Math.min(fragmentLength, encryptedData.remaining());
			ByteBuffer fragment = encryptedData.slice();
			fragment.limit(currentFragmentLength);
			receiver.decrypt(fragment);
			encryptedData.position(encryptedData.position() + currentFragmentLength);
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
