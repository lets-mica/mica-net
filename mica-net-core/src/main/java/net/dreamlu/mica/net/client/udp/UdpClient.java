/*
	Apache License
	Version 2.0, January 2004
	http://www.apache.org/licenses/
*/
package net.dreamlu.mica.net.client.udp;

import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.core.intf.UdpChannel;
import net.dreamlu.mica.net.core.intf.UdpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 标准 Java NIO UDP 客户端，偏生产级实现。
 * <p>
 * 单 DatagramChannel + Selector 读取服务端响应；发送采用无锁队列 +
 * 发送线程，避免在业务线程上阻塞 I/O。
 * <p>
 * 该实现与 mica-net 的 TCP / UDP 框架完全解耦，
 * 仅依赖 JDK NIO 与 SLF4J。
 * <p>
 * 业务回调中拿到的会话类型是 {@link UdpChannel}（接口），实际对象为
 * {@link UdpClientChannel}，可按需强转。
 *
 * @author L.cm
 */
public class UdpClient implements Closeable, Runnable {
	private static final Logger log = LoggerFactory.getLogger(UdpClient.class);
	private final UdpClientConfig config;
	private final UdpHandler handler;
	private final InetSocketAddress serverAddress;
	private final ExecutorService workerPool;
	private final boolean ownsWorkerPool;
	private final UdpClientChannel clientChannel;
	private DatagramChannel channel;
	private Selector selector;
	private Thread sendThread;
	private final LinkedBlockingQueue<ByteBuffer> sendQueue = new LinkedBlockingQueue<>();
	private volatile boolean running;

	public UdpClient(UdpClientConfig config, UdpHandler handler) {
		if (config == null) {
			throw new IllegalArgumentException("config must not be null");
		}
		if (handler == null) {
			throw new IllegalArgumentException("handler must not be null");
		}
		this.config = config;
		this.handler = handler;
		this.serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
		this.clientChannel = new UdpClientChannel(config, handler, serverAddress, sendQueue);
		ExecutorService provided = config.getWorkerPool();
		if (provided != null) {
			this.workerPool = provided;
			this.ownsWorkerPool = false;
		} else {
			this.workerPool = createDefaultPool(config.getHost(), config.getPort());
			this.ownsWorkerPool = true;
		}
	}

	private static ExecutorService createDefaultPool(String host, int port) {
		String prefix = "udp-client-worker-" + host + ':' + port;
		return Executors.newFixedThreadPool(2, new ThreadFactory() {
			private final AtomicInteger seq = new AtomicInteger();

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, prefix + '-' + seq.incrementAndGet());
				t.setDaemon(true);
				return t;
			}
		});
	}

	public synchronized void start() throws IOException {
		if (running) {
			return;
		}
		selector = Selector.open();
		channel = DatagramChannel.open();
		channel.configureBlocking(false);
		int bufSize = Math.max(config.getReadBufferSize() * 4, 64 * 1024);
		channel.setOption(StandardSocketOptions.SO_RCVBUF, bufSize);
		channel.setOption(StandardSocketOptions.SO_SNDBUF, bufSize);
		channel.connect(serverAddress);
		channel.register(selector, SelectionKey.OP_READ);
		sendThread = new Thread(this::sendLoop, "udp-client-sender");
		sendThread.setDaemon(true);
		sendThread.start();
		running = true;
		Thread t = new Thread(this, "udp-client-receiver");
		t.setDaemon(true);
		t.start();
		log.info("NIO UDP client started, target {}:{}", config.getHost(), config.getPort());
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
						handleRead();
					}
				}
			} catch (ClosedSelectorException e) {
				return;
			} catch (Throwable e) {
				if (running) {
					log.error("udp client selector loop error", e);
				}
			}
		}
	}

	private void handleRead() {
		while (true) {
			ByteBuffer buf = ByteBuffer.allocate(config.getReadBufferSize());
			int read;
			try {
				read = channel.read(buf);
			} catch (IOException e) {
				log.error("udp client read error", e);
				return;
			}
			if (read <= 0) {
				return;
			}
			buf.flip();
			final ByteBuffer copy = ByteBuffer.allocate(buf.remaining());
			copy.put(buf);
			copy.flip();
			final UdpChannel session = clientChannel;
			workerPool.execute(() -> {
				try {
					Packet packet = handler.decode(copy, copy.limit(), copy.position(), copy.remaining(), session);
					if (packet == null) {
						return;
					}
					handler.handler(packet, session);
				} catch (Throwable e) {
					log.error("udp client handler error", e);
				}
			});
		}
	}

	/**
	 * 发送业务包：内部使用 {@link UdpHandler#encode} 编码后入发送队列。
	 *
	 * @param packet 业务包
	 * @return 是否成功入队
	 */
	public boolean send(Packet packet) {
		if (!running) {
			return false;
		}
		if (packet == null) {
			return false;
		}
		ByteBuffer encoded = handler.encode(packet, config, clientChannel);
		if (encoded == null) {
			return false;
		}
		boolean offered = sendQueue.offer(encoded);
		if (offered) {
			selector.wakeup();
		}
		return offered;
	}

	private void sendLoop() {
		while (running || !sendQueue.isEmpty()) {
			ByteBuffer buf;
			try {
				buf = sendQueue.poll(200, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			if (buf == null) {
				continue;
			}
			try {
				channel.write(buf);
			} catch (IOException e) {
				log.error("udp client send error", e);
			}
		}
	}

	public boolean isRunning() {
		return running;
	}

	public UdpClientConfig getConfig() {
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
					log.warn("udp client worker pool did not terminate in 5s, forcing shutdownNow");
					workerPool.shutdownNow();
				}
			} catch (InterruptedException e) {
				workerPool.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
		if (sendThread != null) {
			sendThread.interrupt();
		}
		log.info("NIO UDP client stopped, target {}:{}", config.getHost(), config.getPort());
	}
}
