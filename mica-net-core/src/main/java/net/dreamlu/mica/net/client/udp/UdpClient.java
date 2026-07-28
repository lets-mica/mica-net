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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 标准 Java NIO UDP 客户端，偏生产级实现。
 * <p>
 * 单 DatagramChannel + Selector 读取服务端响应；发送采用有界队列 +
 * 发送线程，避免在业务线程上阻塞 I/O。
 * <p>
 * 该实现与 mica-net 的 TCP / UDP 框架完全解耦，
 * 仅依赖 JDK NIO 与 SLF4J。
 * <p>
 * 业务回调中拿到的会话类型是 {@link UdpChannel}（接口），实际对象为
 * {@link UdpClientChannel}，可按需强转。
 * <p>
 * 解码语义：以单个 datagram 为界循环调用 {@code decode}；跨 datagram 半包不拼接。
 * 业务 {@code decode} 返回 Packet 后应推进 position，否则只处理一帧。
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
	private final AtomicBoolean running = new AtomicBoolean(false);
	private final LinkedBlockingQueue<ByteBuffer> sendQueue;
	private final UdpClientChannel clientChannel;
	private final AtomicLong droppedSendCount = new AtomicLong();
	private DatagramChannel channel;
	private Selector selector;
	private Thread sendThread;
	private Thread receiveThread;

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
		this.sendQueue = new LinkedBlockingQueue<>(config.getSendQueueCapacity());
		this.clientChannel = new UdpClientChannel(config, handler, serverAddress, sendQueue, running);
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
		if (running.get()) {
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
		// 必须先置 running，再启动 sendLoop，避免空队列时发送线程直接退出
		running.set(true);
		sendThread = new Thread(this::sendLoop, "udp-client-sender");
		sendThread.setDaemon(true);
		sendThread.start();
		receiveThread = new Thread(this, "udp-client-receiver");
		receiveThread.setDaemon(true);
		receiveThread.start();
		log.info("NIO UDP client started, target {}:{}", config.getHost(), config.getPort());
	}

	@Override
	public void run() {
		while (running.get()) {
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
				if (running.get()) {
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
			if (buf.limit() == buf.capacity()) {
				log.warn("udp datagram may be truncated, bufferSize={}", buf.capacity());
			}
			final ByteBuffer copy = ByteBuffer.allocate(buf.remaining());
			copy.put(buf);
			copy.flip();
			final UdpChannel session = clientChannel;
			try {
				workerPool.execute(() -> {
					try {
						dispatch(session, copy);
					} catch (Throwable e) {
						log.error("udp client handler error", e);
					}
				});
			} catch (RejectedExecutionException e) {
				log.warn("udp client handler rejected");
			}
		}
	}

	private void dispatch(UdpChannel session, ByteBuffer data) throws Exception {
		while (data.hasRemaining()) {
			int before = data.position();
			Packet packet = handler.decode(data, data.limit(), before, data.remaining(), session);
			if (packet == null) {
				return;
			}
			handler.handler(packet, session);
			if (data.position() == before) {
				return;
			}
		}
	}

	/**
	 * 发送业务包：内部使用 {@link UdpHandler#encode} 编码后入发送队列。
	 *
	 * @param packet 业务包
	 * @return 是否成功入队
	 */
	public boolean send(Packet packet) {
		return clientChannel.send(packet);
	}

	private void sendLoop() {
		while (running.get() || !sendQueue.isEmpty()) {
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
			if (channel == null || !channel.isOpen()) {
				droppedSendCount.incrementAndGet();
				continue;
			}
			try {
				int written = channel.write(buf);
				if (written <= 0) {
					droppedSendCount.incrementAndGet();
					log.warn("udp client send dropped (write returned {})", written);
				}
			} catch (IOException e) {
				droppedSendCount.incrementAndGet();
				log.error("udp client send error", e);
			}
		}
	}

	public boolean isRunning() {
		return running.get();
	}

	public UdpClientConfig getConfig() {
		return config;
	}

	/**
	 * 累计因 write 返回 0 / IO 错误 / channel 已关闭而丢弃的发包数。
	 *
	 * @return 丢弃次数
	 */
	public long getDroppedSendCount() {
		return droppedSendCount.get();
	}

	@Override
	public synchronized void close() {
		if (!running.getAndSet(false)) {
			return;
		}
		if (selector != null) {
			selector.wakeup();
		}
		// 先等发送线程排空队列，再关 channel，避免对已关闭 socket 写
		if (sendThread != null) {
			try {
				sendThread.join(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				sendThread.interrupt();
			}
		}
		if (receiveThread != null) {
			try {
				receiveThread.join(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
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
		log.info("NIO UDP client stopped, target {}:{}", config.getHost(), config.getPort());
	}
}
