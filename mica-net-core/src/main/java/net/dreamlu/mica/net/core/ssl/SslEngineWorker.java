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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
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
	/**
	 * wrap 未能推进时的最大重试次数，防止握手状态机异常时死循环
	 */
	private static final int MAX_WRAP_RETRY = 8;

	private final ChannelContext channelContext;
	private final SSLEngine engine;
	private final SslBuffers buffers;
	private final Consumer<ByteBuffer> encryptedDataConsumer;
	private final Consumer<ByteBuffer> plainDataConsumer;
	private final Runnable handshakeCompletedListener;
	private final Runnable sessionClosedListener;
	/**
	 * SSLEngine 不是线程安全的：读线程调用 unwrap、写线程调用 wrap，
	 * 并发访问会导致序号/密钥状态错乱，表现为 Tag mismatch、握手期发出非法记录等。
	 * 这里用连接级锁把引擎访问串行化。
	 */
	private final ReentrantLock engineLock = new ReentrantLock();
	private volatile boolean handshakeStarted;
	private volatile boolean handshakeCompleted;
	/**
	 * 握手完成通知只触发一次
	 */
	private final AtomicBoolean handshakeNotified = new AtomicBoolean(false);
	/**
	 * 握手已完成但回调尚未触发（等待释放 engineLock 后再触发）
	 */
	private volatile boolean handshakeCompletedPending;
	/**
	 * 本端是否已主动关闭会话。
	 * <p>
	 * 主动关闭后引擎自身的 inbound 也会被关闭，此时 unwrap 同样返回 CLOSED，
	 * 但这不是"对端关闭"，不能再触发 session closed 回调，否则会造成误报。
	 * </p>
	 */
	private volatile boolean closed;

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
		engineLock.lock();
		try {
			engine.beginHandshake();
			driveHandshake();
		} finally {
			engineLock.unlock();
			// 同 encrypt()：握手完成回调放到锁外触发。
			fireHandshakeCompleted();
		}
	}

	boolean isHandshakeCompleted() {
		return handshakeCompleted;
	}

	ByteBuffer encrypt(ByteBuffer plainData) throws SSLException {
		engineLock.lock();
		try {
			return doEncrypt(plainData);
		} finally {
			engineLock.unlock();
			// 握手完成回调必须在释放 engineLock 之后触发：
			// 回调里往往会同步发送业务包（如 mica-mqtt 用 Tio.bSend 发 CONNECT），
			// 而发送需要 engineLock 加密，若在锁内触发会形成自死锁（客户端表现为 TLS 一直连不上）。
			fireHandshakeCompleted();
		}
	}

	private ByteBuffer doEncrypt(ByteBuffer plainData) throws SSLException {
		// 握手未完成时不能加密业务数据:此时 wrap 只会产出握手记录,业务数据不会被消费,
		// 握手记录还会被误当作业务密文丢弃,导致对端收不到 ClientHello 而握手卡死。
		// 正常流程下握手完成前业务包会滞留在 sendRunnable 的待发送队列里,不会走到这里。
		if (!handshakeCompleted) {
			throw new SSLException("SSL 握手尚未完成，无法加密业务数据");
		}
		// 单次业务数据可能跨越多个 TLS Record,需要循环 wrap 并合并
		List<ByteBuffer> encryptedBuffers = new ArrayList<>();
		int retry = 0;
		while (plainData.hasRemaining()) {
			int position = plainData.position();
			WrapResult wrapResult = wrap(plainData);
			if (wrapResult.encryptedData.hasRemaining()) {
				encryptedBuffers.add(wrapResult.encryptedData);
			}
			SSLEngineResult.HandshakeStatus handshakeStatus = wrapResult.result.getHandshakeStatus();
			// wrap 后状态可能进入握手阶段,继续驱动状态机
			handleHandshakeStatus(handshakeStatus);
			if (wrapResult.result.getStatus() == SSLEngineResult.Status.CLOSED) {
				break;
			}
			// 握手尚未完成时 JDK 不会消费应用数据(bytesConsumed=0),此时应先推进握手状态机再重试,
			// 直接判定失败会丢失业务包(表现为 SSLEngine.wrap made no progress)
			if (plainData.position() == position && !wrapResult.encryptedData.hasRemaining()) {
				if (!handshakeCompleted) {
					// 握手未完成就加密业务数据,通常意味着握手被提前判定完成(业务包被提前放行),
					// 此时对端收到的是非法的 TLS 记录,这里给出明确的错误信息便于定位
					throw new SSLException("SSL 握手尚未完成，无法加密业务数据");
				}
				if (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP
					|| handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
					if (++retry > MAX_WRAP_RETRY) {
						throw new SSLException("SSLEngine.wrap made no progress after " + retry + " retries");
					}
					continue;
				}
				// wrap 没推进也没产生数据,避免死循环
				throw new SSLException("SSLEngine.wrap made no progress");
			}
			retry = 0;
		}
		return ByteBufferUtil.combine(encryptedBuffers);
	}

	void decrypt(ByteBuffer encryptedData) throws SSLException {
		DecryptOutcome outcome = new DecryptOutcome();
		boolean success = false;
		try {
			engineLock.lock();
			try {
				doDecrypt(encryptedData, outcome);
				success = true;
			} finally {
				engineLock.unlock();
				// 同 encrypt()：握手完成回调放到锁外触发，避免回调内同步发包与加密互锁。
				fireHandshakeCompleted();
			}
		} finally {
			// 明文数据必须在释放 engineLock 之后再交给业务：
			// decode/handler 里通常伴随同步发送（如 mica-mqtt 处理消息后回复），
			// 而发送线程加密数据需要同一把引擎锁，锁内回调会形成互相等待。
			// 即使后续解密抛出异常，已经解出来的明文也要交付，避免丢数据。
			for (ByteBuffer plainData : outcome.plainDataList) {
				plainDataConsumer.accept(plainData);
			}
			if (success && outcome.sessionClosed) {
				sessionClosedListener.run();
			}
		}
	}

	/**
	 * 一次解密的产出，需要在释放 engineLock 之后再交给业务处理
	 */
	private static final class DecryptOutcome {
		private final List<ByteBuffer> plainDataList = new ArrayList<>(2);
		private boolean sessionClosed;
	}

	private void doDecrypt(ByteBuffer encryptedData, DecryptOutcome outcome) throws SSLException {
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
					if (!handshakeCompleted) {
						// 兜底：能解出应用数据说明握手实际已经完成，
						// 避免握手完成信号丢失导致业务包永久滞留在待发送队列里
						completeHandshake();
					}
					outcome.plainDataList.add(SslBuffers.toReadBuffer(applicationBuffer));
				}
				if (result.getStatus() == SSLEngineResult.Status.CLOSED) {
					if (!closed) {
						outcome.sessionClosed = true;
					}
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
		// 标记为本端主动关闭，之后引擎自身触发的 CLOSED 不再回调 session closed
		closed = true;
		engineLock.lock();
		try {
			engine.closeOutbound();
			// 驱动一次 wrap，把 close_notify 记录发出去，让对端知道是主动断开而不是异常断流
			driveHandshake();
			engine.closeInbound();
		} catch (SSLException e) {
			log.debug("{} 关闭SSL会话时忽略异常", channelContext, e);
		} finally {
			engineLock.unlock();
			fireHandshakeCompleted();
		}
	}

	/**
	 * 关闭 outbound 并产出 close_notify 记录。
	 * <p>
	 * 与 {@link #close()} 的区别：这里不把 close_notify 交给发送队列，而是把记录本身返回给调用方，
	 * 由调用方直接写入 socket。关闭流程里发送任务已被取消、socket 也即将关闭，
	 * 走发送队列的 close_notify 会被丢弃，发不出去。
	 * </p>
	 *
	 * @return close_notify 记录；引擎已关闭或无数据可发时返回 null
	 */
	ByteBuffer closeOutboundAndGetCloseNotify() {
		closed = true;
		engineLock.lock();
		try {
			engine.closeOutbound();
			// closeOutbound 之后的第一次 wrap 会产出 close_notify 记录
			ByteBuffer networkBuffer = buffers.packetBuffer();
			engine.wrap(EMPTY_BUFFER.duplicate(), networkBuffer);
			ByteBuffer closeNotify = networkBuffer.hasRemaining() ? SslBuffers.toReadBuffer(networkBuffer) : null;
			try {
				// 对端还没回 close_notify 时，JDK 会抛 SSLException，这里忽略：
				// 本端正是要主动关闭，不关心对端是否回通知
				engine.closeInbound();
			} catch (SSLException e) {
				log.debug("{} 关闭SSL入站时忽略异常", channelContext, e);
			}
			return closeNotify;
		} catch (SSLException e) {
			log.debug("{} 产出close_notify时忽略异常", channelContext, e);
			return null;
		} finally {
			engineLock.unlock();
			fireHandshakeCompleted();
		}
	}

	void terminate() {
		closed = true;
		engineLock.lock();
		try {
			engine.closeOutbound();
			engine.closeInbound();
		} catch (SSLException e) {
			log.debug("{} 终止SSL会话时忽略异常", channelContext, e);
		} finally {
			engineLock.unlock();
			fireHandshakeCompleted();
		}
	}

	private void handleHandshakeStatus(SSLEngineResult.HandshakeStatus handshakeStatus) throws SSLException {
		if (log.isDebugEnabled()) {
			log.debug("{} 握手状态:{}", channelContext, handshakeStatus);
		}
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
					if (wrapResult.result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED
						|| engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
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
					// NOT_HANDSHAKING 只表示"当前没有握手在进行",不能作为握手成功的判据。
					// 握手完成的唯一判据是 FINISHED;此处若提前宣告完成,业务包会在握手真正结束前被加密并发出,
					// 对端会收到非法的 TLS 记录(握手期收到 application_data),表现为 bad record type。
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
		handshakeCompletedPending = true;
		fireHandshakeCompleted();
	}

	/**
	 * 触发握手完成回调。
	 * <p>
	 * 若当前线程持有 engineLock（解密/加密过程中完成握手），则延迟到锁释放后再触发，
	 * 否则回调里同步发包会与加密争锁形成死锁。
	 */
	private void fireHandshakeCompleted() {
		if (!handshakeCompletedPending) {
			return;
		}
		if (engineLock.isHeldByCurrentThread()) {
			// 仍在锁内，等调用方释放锁后再触发
			return;
		}
		handshakeCompletedPending = false;
		if (handshakeNotified.compareAndSet(false, true)) {
			handshakeCompletedListener.run();
		}
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
