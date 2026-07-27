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
import java.util.function.Consumer;
import javax.net.ssl.*;

import net.dreamlu.mica.net.utils.buffer.ByteBufferUtil;
import net.dreamlu.mica.net.utils.hutool.StrUtil;
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
		// 单次业务数据可能跨越多个 TLS Record,需要循环 wrap 并合并
		List<ByteBuffer> encryptedBuffers = new ArrayList<>();
		while (plainData.hasRemaining()) {
			int position = plainData.position();
			WrapResult wrapResult = wrap(plainData);
			if (wrapResult.encryptedData.hasRemaining()) {
				encryptedBuffers.add(wrapResult.encryptedData);
			}
			// wrap 后状态可能进入握手阶段,继续驱动状态机
			handleHandshakeStatus(wrapResult.result.getHandshakeStatus());
			if (wrapResult.result.getStatus() == SSLEngineResult.Status.CLOSED) {
				break;
			}
			// wrap 没推进也没产生数据,避免死循环
			if (plainData.position() == position && !wrapResult.encryptedData.hasRemaining()) {
				throw new SSLException("SSLEngine.wrap made no progress");
			}
		}
		return ByteBufferUtil.combine(encryptedBuffers);
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
			// 包缓冲不足,扩容后重试
			networkBuffer = buffers.growPacketBuffer(networkBuffer);
		}
	}

	private void completeHandshake() {
		// 保证握手完成回调只触发一次
		if (handshakeCompleted) {
			return;
		}
		handshakeCompleted = true;
		handshakeCompletedListener.run();
	}

	private static SSLEngine createEngine(SSLContext context, boolean clientMode, SslConfig sslConfig) {
		SSLEngine engine = context.createSSLEngine();
		// 1. 配置引擎角色:客户端主动握手,服务端等待对端 ClientHello。
		engine.setUseClientMode(clientMode);
		// 2. 仅服务端需要决定是否要求/可选客户端证书(mTLS 场景):
		//    NONE     - 不请求客户端证书,握手不验证对端身份
		//    OPTIONAL - 请求但不强制,缺失证书也能完成握手
		//    REQUIRE  - 强制要求,缺失则握手失败
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
					throw new IllegalArgumentException("Unknown SSL auth " + clientAuth);
			}
		}
		// 3. 取出当前默认 SSLParameters,在原对象上叠加用户自定义配置,
		//    避免覆盖 JDK 默认启用的安全算法与扩展,只做增量修改。
		SSLParameters sslParameters = engine.getSSLParameters();
		// 3.1 限定协议版本(如 TLSv1.2 / TLSv1.3),未设置时使用 JDK 默认。
		String[] protocols = sslConfig.getProtocols();
		if (protocols != null && protocols.length > 0) {
			sslParameters.setProtocols(protocols);
		}
		// 3.2 限定密码套件,未设置时使用 JDK 默认;顺序会影响优先级。
		String[] cipherSuites = sslConfig.getCipherSuites();
		if (cipherSuites != null && cipherSuites.length > 0) {
			sslParameters.setCipherSuites(cipherSuites);
		}
		// 4. 仅客户端模式下的额外配置:端点校验和 SNI。
		//    服务端使用这些字段没有意义,且部分实现在 server 模式下拒绝设置。
		if (engine.getUseClientMode()) {
			// 4.1 端点识别算法(如 "HTTPS"/"LDAPS"),开启后会对证书主机名做校验,
			//      防止中间人攻击;为空表示不启用,需要业务自行校验证书。
			String endpointIdentificationAlgorithm = sslConfig.getEndpointIdentificationAlgorithm();
			if (StrUtil.isNotBlank(endpointIdentificationAlgorithm)) {
				sslParameters.setEndpointIdentificationAlgorithm(endpointIdentificationAlgorithm);
			}
			// 4.2 SNI(Server Name Indication),TLS 扩展用于在握手时告知服务端要访问的虚拟主机,
			//     让单 IP 多证书的服务端选择正确的证书;为空表示不发送 SNI。
			String serverName = sslConfig.getServerName();
			if (StrUtil.isNotBlank(serverName)) {
				List<SNIServerName> serverNames = new ArrayList<>(1);
				serverNames.add(new SNIHostName(serverName));
				sslParameters.setServerNames(serverNames);
			}
		} else {
			// 是否按客户端顺序选择密码套件(true 时服务端遵循客户端列表顺序),
			Boolean useCipherSuitesOrder = sslConfig.getUseCipherSuitesOrder();
			if (useCipherSuitesOrder != null) {
				sslParameters.setUseCipherSuitesOrder(useCipherSuitesOrder);
			}
		}
		// 5. 将叠加后的 SSLParameters 一次性回写到引擎,
		//    必须在 beginHandshake() 之前完成,握手启动后修改可能无效。
		engine.setSSLParameters(sslParameters);
		// 6. 提供给用户的最后兜底扩展点,可在拿到引擎后做任意定制
		//    (如设置算法白名单、调整最大片长度等),位置在 setSSLParameters 之后以保证自定义项不被覆盖。
		SSLEngineCustomizer customizer = sslConfig.getSslEngineCustomizer();
		if (customizer != null) {
			customizer.customize(engine);
		}
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
