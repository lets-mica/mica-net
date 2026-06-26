/*
	Apache License
	Version 2.0, January 2004
	http://www.apache.org/licenses/
*/
package net.dreamlu.mica.net.server.udp;

import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.core.intf.UdpChannel;
import net.dreamlu.mica.net.core.intf.UdpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 标准 Java NIO UDP 服务端，偏生产级实现。
 * <p>
 * 单 Selector 线程负责 I/O，业务逻辑通过 {@link UdpHandler} 回调，
 * 内部使用业务线程池把处理从 Selector 线程剥离，避免业务阻塞 I/O 循环。
 * <p>
 * 该实现与 mica-net 的 TCP / UDP 框架完全解耦，
 * 仅依赖 JDK NIO 与 SLF4J，可作为 UDP 协议的参考实现。
 * <p>
 * 业务回调中拿到的会话类型是 {@link UdpChannel}（接口），实际对象为
 * {@link UdpServerChannel}，可按需强转。
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
	private DatagramChannel channel;
	private Selector selector;
	private volatile boolean running;

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
		if (running) {
			return;
		}
		final int port = config.getPort();
		final int readBufferSize = config.getReadBufferSize();
		selector = Selector.open();
		channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.setOption(StandardSocketOptions.SO_RCVBUF, Math.max(readBufferSize * 4, 64 * 1024));
		channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		channel.bind(bindAddress);
		channel.register(selector, SelectionKey.OP_READ);
		running = true;
		Thread t = new Thread(this, "udp-server-accept-" + port);
		t.setDaemon(false);
		t.start();
		log.info("NIO UDP server started on port {}", port);
	}

	@Override
	public void run() {
		while (running) {
			try {
				if (selector.select(500) <= 0) {
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
			} catch (Throwable e) {
				if (running) {
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
			final InetSocketAddress remoteAddr = (InetSocketAddress) remote;
			final ByteBuffer copy = ByteBuffer.allocate(buf.remaining());
			copy.put(buf);
			copy.flip();
			// 同一对端复用同一个 UdpServerChannel 实例，避免每包分配 + 让 handler 维护 per-peer 状态。
			final UdpServerChannel session = peerChannels.computeIfAbsent(remoteAddr,
				addr -> new UdpServerChannel(config, handler, ch, addr));
			workerPool.execute(() -> {
				try {
					dispatch(session, copy);
				} catch (Throwable e) {
					log.error("udp handler error from {}", remoteAddr, e);
				}
			});
		}
	}

	/**
	 * 解码循环：UdpHandler.decode 可能需要累积多包；这里以单包为最小演示单位，
	 * 真实协议中应支持粘包 / 半包。
	 */
	private void dispatch(UdpChannel channel, ByteBuffer data) throws Exception {
		Packet packet = handler.decode(data, data.limit(), data.position(), data.remaining(), channel);
		if (packet == null) {
			return;
		}
		handler.handler(packet, channel);
	}

	public boolean isRunning() {
		return running;
	}

	public UdpServerConfig getConfig() {
		return config;
	}

	@Override
	public synchronized void close() {
		if (!running) {
			return;
		}
		running = false;
		if (selector != null) {
			selector.wakeup();
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
		// 优雅关闭 worker pool：先等待 in-flight 任务完成，超时则强制 shutdownNow。
		// 自有 pool 才管生命周期，用户注入的 pool 由用户自己管。
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
		log.info("NIO UDP server stopped on port {}", config.getPort());
	}
}
