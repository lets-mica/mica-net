/*
	Apache License
	Version 2.0, January 2004
	http://www.apache.org/licenses/
*/
package net.dreamlu.mica.net.server.udp;

import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.core.intf.UdpChannel;
import net.dreamlu.mica.net.core.intf.UdpHandler;
import net.dreamlu.mica.net.server.udp.UdpServerChannel.OutboundDatagram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 标准 Java NIO UDP 服务端，偏生产级实现。
 * <p>
 * 单 Selector 线程负责读；业务经 {@link UdpHandler} 回调，按对端串行调度到业务线程池；
 * 回包经有界发送队列由单发送线程写出，避免多线程争用 {@code DatagramChannel}。
 * <p>
 * 同一对端地址复用 {@link UdpServerChannel}，受 {@link UdpServerConfig#getMaxPeers()}
 * 与 {@link UdpServerConfig#getPeerIdleTimeoutMs()} 约束。
 * <p>
 * 解码语义：以单个 datagram 为界循环调用 {@code decode}；跨 datagram 半包不拼接。
 * 业务 {@code decode} 返回 Packet 后应推进 position，否则只处理一帧。
 *
 * @author L.cm
 */
public class UdpServer implements Closeable, Runnable {
	private static final Logger log = LoggerFactory.getLogger(UdpServer.class);
	private final UdpServerConfig config;
	private final UdpHandler handler;
	private final InetSocketAddress bindAddress;
	private final ExecutorService workerPool;
	private final boolean ownsWorkerPool;
	private final ConcurrentMap<InetSocketAddress, UdpServerChannel> peerChannels = new ConcurrentHashMap<>();
	private final LinkedBlockingQueue<OutboundDatagram> sendQueue;
	private final AtomicBoolean running = new AtomicBoolean(false);
	private final AtomicLong droppedInboundCount = new AtomicLong();
	private DatagramChannel channel;
	private Selector selector;
	private Thread ioThread;
	private Thread sendThread;
	private long lastPeerPurgeNanos;

	public UdpServer(UdpServerConfig config, UdpHandler handler) {
		if (config == null) {
			throw new IllegalArgumentException("config must not be null");
		}
		if (handler == null) {
			throw new IllegalArgumentException("handler must not be null");
		}
		this.config = config;
		this.handler = handler;
		this.bindAddress = new InetSocketAddress(config.getPort());
		this.sendQueue = new LinkedBlockingQueue<>(config.getSendQueueCapacity());
		ExecutorService provided = config.getWorkerPool();
		if (provided != null) {
			this.workerPool = provided;
			this.ownsWorkerPool = false;
		} else {
			this.workerPool = createDefaultPool(config.getPort(), config.getWorkerThreads());
			this.ownsWorkerPool = true;
		}
	}

	private static ExecutorService createDefaultPool(int port, int workerThreads) {
		return Executors.newFixedThreadPool(workerThreads, new ThreadFactory() {
			private final AtomicInteger seq = new AtomicInteger();

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "udp-server-worker-" + port + '-' + seq.incrementAndGet());
				t.setDaemon(true);
				return t;
			}
		});
	}

	public synchronized void start() throws IOException {
		if (running.get()) {
			return;
		}
		final int port = config.getPort();
		final int readBufferSize = config.getReadBufferSize();
		selector = Selector.open();
		channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.setOption(StandardSocketOptions.SO_RCVBUF, Math.max(readBufferSize * 4, 64 * 1024));
		channel.setOption(StandardSocketOptions.SO_SNDBUF, Math.max(readBufferSize * 4, 64 * 1024));
		channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		channel.bind(bindAddress);
		channel.register(selector, SelectionKey.OP_READ);
		running.set(true);
		lastPeerPurgeNanos = System.nanoTime();
		sendThread = new Thread(this::sendLoop, "udp-server-sender-" + port);
		sendThread.setDaemon(true);
		sendThread.start();
		ioThread = new Thread(this, "udp-server-accept-" + port);
		ioThread.setDaemon(false);
		ioThread.start();
		log.info("NIO UDP server started on port {}", port);
	}

	@Override
	public void run() {
		while (running.get()) {
			try {
				if (selector.select(500) <= 0) {
					purgeIdlePeersIfNeeded();
					continue;
				}
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey key = it.next();
					it.remove();
					if (!key.isValid()) {
						continue;
					}
					if (key.isReadable()) {
						handleRead((DatagramChannel) key.channel());
					}
				}
				purgeIdlePeersIfNeeded();
			} catch (ClosedSelectorException e) {
				return;
			} catch (Throwable e) {
				if (running.get()) {
					log.error("udp server selector loop error", e);
				}
			}
		}
	}

	private void handleRead(DatagramChannel ch) {
		while (true) {
			ByteBuffer buf = ByteBuffer.allocate(config.getReadBufferSize());
			SocketAddress remote;
			try {
				remote = ch.receive(buf);
			} catch (IOException e) {
				log.error("udp receive error", e);
				return;
			}
			if (remote == null) {
				return;
			}
			buf.flip();
			if (buf.limit() == buf.capacity()) {
				log.warn("udp datagram may be truncated from {}, bufferSize={}", remote, buf.capacity());
			}
			final InetSocketAddress remoteAddr = (InetSocketAddress) remote;
			final UdpServerChannel session = resolvePeer(remoteAddr);
			if (session == null) {
				droppedInboundCount.incrementAndGet();
				continue;
			}
			session.touch();
			final ByteBuffer copy = ByteBuffer.allocate(buf.remaining());
			copy.put(buf);
			copy.flip();
			try {
				session.executeSerially(workerPool, () -> {
					try {
						dispatch(session, copy);
					} catch (Throwable e) {
						log.error("udp handler error from {}", remoteAddr, e);
					}
				});
			} catch (RejectedExecutionException e) {
				log.warn("udp handler rejected from {}", remoteAddr);
			}
		}
	}

	private UdpServerChannel resolvePeer(InetSocketAddress remoteAddr) {
		UdpServerChannel existing = peerChannels.get(remoteAddr);
		if (existing != null) {
			if (!existing.isClosed()) {
				return existing;
			}
			peerChannels.remove(remoteAddr, existing);
		}
		if (peerChannels.size() >= config.getMaxPeers()) {
			log.warn("udp maxPeers={} reached, drop datagram from {}", config.getMaxPeers(), remoteAddr);
			return null;
		}
		UdpServerChannel created = new UdpServerChannel(
			config,
			handler,
			remoteAddr,
			running,
			this::offerSend,
			peerChannels::remove
		);
		UdpServerChannel raced = peerChannels.putIfAbsent(remoteAddr, created);
		if (raced != null) {
			return raced.isClosed() ? null : raced;
		}
		if (peerChannels.size() > config.getMaxPeers()) {
			peerChannels.remove(remoteAddr, created);
			log.warn("udp maxPeers={} raced, drop datagram from {}", config.getMaxPeers(), remoteAddr);
			return null;
		}
		return created;
	}

	private boolean offerSend(OutboundDatagram datagram) {
		if (!running.get()) {
			return false;
		}
		return sendQueue.offer(datagram);
	}

	private void sendLoop() {
		while (running.get() || !sendQueue.isEmpty()) {
			OutboundDatagram outbound;
			try {
				outbound = sendQueue.poll(200, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			if (outbound == null) {
				continue;
			}
			if (channel == null || !channel.isOpen()) {
				continue;
			}
			try {
				int sent = channel.send(outbound.buffer, outbound.remote);
				if (sent <= 0) {
					UdpServerChannel peer = peerChannels.get(outbound.remote);
					if (peer != null) {
						peer.incrementDroppedSend();
					}
					log.warn("udp send dropped to {} (send returned {})", outbound.remote, sent);
				}
			} catch (IOException e) {
				UdpServerChannel peer = peerChannels.get(outbound.remote);
				if (peer != null) {
					peer.incrementDroppedSend();
				}
				log.warn("udp send dropped to {}: {}", outbound.remote, e.getMessage());
			}
		}
	}

	/**
	 * 在单个 datagram 内循环 decode；跨包半包不保留。
	 * decode 返回 Packet 后若未推进 position，则只处理这一帧后退出，避免死循环。
	 */
	private void dispatch(UdpChannel channel, ByteBuffer data) throws Exception {
		while (data.hasRemaining()) {
			int before = data.position();
			Packet packet = handler.decode(data, data.limit(), before, data.remaining(), channel);
			if (packet == null) {
				return;
			}
			handler.handler(packet, channel);
			if (data.position() == before) {
				return;
			}
		}
	}

	private void purgeIdlePeersIfNeeded() {
		long timeoutMs = config.getPeerIdleTimeoutMs();
		if (timeoutMs <= 0) {
			return;
		}
		long now = System.nanoTime();
		if (now - lastPeerPurgeNanos < TimeUnit.SECONDS.toNanos(1)) {
			return;
		}
		lastPeerPurgeNanos = now;
		long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMs);
		peerChannels.entrySet().removeIf(e ->
			e.getValue().isClosed() || now - e.getValue().getLastActiveNanos() > timeoutNanos);
	}

	public boolean isRunning() {
		return running.get();
	}

	public UdpServerConfig getConfig() {
		return config;
	}

	/**
	 * 当前缓存的对端会话数（测试 / 监控用）。
	 *
	 * @return peer 数量
	 */
	public int getPeerChannelCount() {
		return peerChannels.size();
	}

	/**
	 * 因 maxPeers 满而丢弃的入站 datagram 数。
	 *
	 * @return 丢弃次数
	 */
	public long getDroppedInboundCount() {
		return droppedInboundCount.get();
	}

	@Override
	public synchronized void close() {
		if (!running.getAndSet(false)) {
			return;
		}
		if (selector != null) {
			selector.wakeup();
		}
		if (ioThread != null) {
			try {
				ioThread.join(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		if (ownsWorkerPool && workerPool != null) {
			workerPool.shutdown();
			try {
				if (!workerPool.awaitTermination(5, TimeUnit.SECONDS)) {
					log.warn("udp server worker pool did not terminate in 5s, forcing shutdownNow");
					workerPool.shutdownNow();
				}
			} catch (InterruptedException e) {
				workerPool.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
		if (sendThread != null) {
			try {
				sendThread.join(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				sendThread.interrupt();
			}
		}
		if (channel != null) {
			try {
				channel.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
		if (selector != null) {
			try {
				selector.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
		sendQueue.clear();
		peerChannels.clear();
		log.info("NIO UDP server stopped on port {}", config.getPort());
	}
}
