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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dreamlu.mica.net.core.ChannelContext;

/**
 * Per-connection SSLEngine state machine.
 *
 * @author L.cm
 */
final class SslEngineWorker {
	private static final Logger log = LoggerFactory.getLogger(SslEngineWorker.class);
	private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

	private final ChannelContext channelContext;
	private final SSLEngine engine;
	private final SslBuffers buffers;
	private final Consumer<ByteBuffer> encryptedDataConsumer;
	private final Consumer<ByteBuffer> plainDataConsumer;
	private final Runnable handshakeCompletedListener;
	private final Runnable sessionClosedListener;
	private boolean handshakeStarted;
	private volatile boolean handshakeCompleted;

	SslEngineWorker(ChannelContext channelContext, SSLContext sslContext, boolean clientMode, SslConfig sslConfig,
					Consumer<ByteBuffer> encryptedDataConsumer, Consumer<ByteBuffer> plainDataConsumer,
					Runnable handshakeCompletedListener, Runnable sessionClosedListener) {
		this.channelContext = channelContext;
		this.engine = createEngine(sslContext, clientMode, sslConfig);
		this.buffers = new SslBuffers(engine.getSession());
		this.encryptedDataConsumer = encryptedDataConsumer;
		this.plainDataConsumer = plainDataConsumer;
		this.handshakeCompletedListener = handshakeCompletedListener;
		this.sessionClosedListener = sessionClosedListener;
	}

	void beginHandshake() throws SSLException {
		if (handshakeStarted) {
			return;
		}
		handshakeStarted = true;
		engine.beginHandshake();
		driveHandshake();
	}

	boolean isHandshakeCompleted() {
		return handshakeCompleted;
	}

	ByteBuffer encrypt(ByteBuffer plainData) throws SSLException {
		List<ByteBuffer> encryptedBuffers = new ArrayList<>();
		while (plainData.hasRemaining()) {
			int position = plainData.position();
			WrapResult wrapResult = wrap(plainData);
			if (wrapResult.encryptedData.hasRemaining()) {
				encryptedBuffers.add(wrapResult.encryptedData);
			}
			handleHandshakeStatus(wrapResult.result.getHandshakeStatus());
			if (wrapResult.result.getStatus() == SSLEngineResult.Status.CLOSED) {
				break;
			}
			if (plainData.position() == position && !wrapResult.encryptedData.hasRemaining()) {
				throw new SSLException("SSLEngine.wrap made no progress");
			}
		}
		return SslBuffers.combine(encryptedBuffers);
	}

	void decrypt(ByteBuffer encryptedData) throws SSLException {
		buffers.appendEncrypted(encryptedData);
		ByteBuffer networkBuffer = buffers.beginUnwrap();
		boolean underflow = false;
		try {
			while (networkBuffer.hasRemaining()) {
				int networkPosition = networkBuffer.position();
				ByteBuffer applicationBuffer = buffers.applicationBuffer();
				SSLEngineResult result;
				while (true) {
					result = engine.unwrap(networkBuffer, applicationBuffer);
					if (result.getStatus() != SSLEngineResult.Status.BUFFER_OVERFLOW) {
						break;
					}
					applicationBuffer = buffers.growApplicationBuffer(applicationBuffer);
				}

				handleHandshakeStatus(result.getHandshakeStatus());
				if (applicationBuffer.position() > 0) {
					plainDataConsumer.accept(SslBuffers.toReadBuffer(applicationBuffer));
				}
				if (result.getStatus() == SSLEngineResult.Status.CLOSED) {
					sessionClosedListener.run();
					return;
				}
				if (result.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
					underflow = true;
					return;
				}
				if (networkBuffer.position() == networkPosition && result.bytesProduced() == 0) {
					return;
				}
			}
		} finally {
			buffers.endUnwrap(underflow);
		}
	}

	void close() {
		engine.closeOutbound();
		try {
			driveHandshake();
			engine.closeInbound();
		} catch (SSLException e) {
			log.debug("{} 关闭SSL会话时忽略异常", channelContext, e);
		}
	}

	void terminate() {
		engine.closeOutbound();
		try {
			engine.closeInbound();
		} catch (SSLException e) {
			log.debug("{} 终止SSL会话时忽略异常", channelContext, e);
		}
	}

	private void handleHandshakeStatus(SSLEngineResult.HandshakeStatus handshakeStatus) throws SSLException {
		if (handshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED) {
			completeHandshake();
		}
		driveHandshake();
	}

	private void driveHandshake() throws SSLException {
		while (true) {
			switch (engine.getHandshakeStatus()) {
				case NEED_TASK:
					Runnable task;
					while ((task = engine.getDelegatedTask()) != null) {
						task.run();
					}
					break;
				case NEED_WRAP:
					WrapResult wrapResult = wrap(EMPTY_BUFFER.duplicate());
					if (wrapResult.encryptedData.hasRemaining()) {
						encryptedDataConsumer.accept(wrapResult.encryptedData);
					}
					if (wrapResult.result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
						completeHandshake();
					}
					if (wrapResult.result.getStatus() == SSLEngineResult.Status.CLOSED) {
						return;
					}
					break;
				case FINISHED:
					completeHandshake();
					return;
				case NOT_HANDSHAKING:
					if (handshakeStarted) {
						completeHandshake();
					}
					return;
				case NEED_UNWRAP:
				default:
					return;
			}
		}
	}

	private WrapResult wrap(ByteBuffer plainData) throws SSLException {
		ByteBuffer networkBuffer = buffers.packetBuffer();
		while (true) {
			SSLEngineResult result = engine.wrap(plainData, networkBuffer);
			if (result.getStatus() != SSLEngineResult.Status.BUFFER_OVERFLOW) {
				return new WrapResult(result, SslBuffers.toReadBuffer(networkBuffer));
			}
			networkBuffer = buffers.growPacketBuffer(networkBuffer);
		}
	}

	private void completeHandshake() {
		if (handshakeCompleted) {
			return;
		}
		handshakeCompleted = true;
		handshakeCompletedListener.run();
	}

	private static SSLEngine createEngine(SSLContext context, boolean clientMode, SslConfig sslConfig) {
		SSLEngine engine = context.createSSLEngine();
		engine.setUseClientMode(clientMode);
		if (!clientMode) {
			ClientAuth clientAuth = sslConfig.getClientAuth();
			switch (clientAuth) {
				case OPTIONAL:
					engine.setWantClientAuth(true);
					break;
				case REQUIRE:
					engine.setNeedClientAuth(true);
					break;
				case NONE:
					break;
				default:
					throw new IllegalArgumentException("Unknown auth " + clientAuth);
			}
		}
		Optional.ofNullable(sslConfig.getSslEngineCustomizer())
			.ifPresent(customizer -> customizer.customize(engine));
		return engine;
	}

	private static final class WrapResult {
		private final SSLEngineResult result;
		private final ByteBuffer encryptedData;

		private WrapResult(SSLEngineResult result, ByteBuffer encryptedData) {
			this.result = result;
			this.encryptedData = encryptedData;
		}
	}
}
