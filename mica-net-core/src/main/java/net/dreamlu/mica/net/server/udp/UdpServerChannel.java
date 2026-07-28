/*
	Apache License
	Version 2.0, January 2004
	http://www.apache.org/licenses/
*/
package net.dreamlu.mica.net.server.udp;

import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.core.intf.UdpChannel;
import net.dreamlu.mica.net.core.intf.UdpHandler;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * UDP 服务端 {@link UdpChannel} 实现。
 * <p>
 * send 入共享发送队列，由 {@link UdpServer} 单发送线程落 socket，
 * 避免多 worker 并发写同一 {@code DatagramChannel}。
 * <p>
 * 同一对端的业务回调通过本类内部任务队列串行执行，保证有状态协议的顺序。
 * <p>
 * 类是 public 但构造器是 package-private：仅 {@link UdpServer} 能创建实例，
 * 业务代码通过 {@link UdpHandler#handler} 回调拿到该对象，按需转成具体类型。
 *
 * @author L.cm
 */
public class UdpServerChannel implements UdpChannel {
	private final UdpServerConfig config;
	private final UdpHandler handler;
	private final InetSocketAddress remote;
	private final AtomicBoolean running;
	private final SendOffer sendOffer;
	private final Consumer<InetSocketAddress> onClosed;
	private final AtomicLong droppedSendCount = new AtomicLong();
	private final ConcurrentLinkedQueue<Runnable> serialTasks = new ConcurrentLinkedQueue<>();
	private final AtomicBoolean serialScheduled = new AtomicBoolean(false);
	private final AtomicBoolean closed = new AtomicBoolean(false);
	private volatile long lastActiveNanos = System.nanoTime();

	UdpServerChannel(UdpServerConfig config,
					 UdpHandler handler,
					 InetSocketAddress remote,
					 AtomicBoolean running,
					 SendOffer sendOffer,
					 Consumer<InetSocketAddress> onClosed) {
		this.config = config;
		this.handler = handler;
		this.remote = remote;
		this.running = running;
		this.sendOffer = sendOffer;
		this.onClosed = onClosed;
	}

	@Override
	public UdpServerConfig getConfig() {
		return config;
	}

	@Override
	public InetSocketAddress remoteAddress() {
		return remote;
	}

	/**
	 * 刷新最近活跃时间（收到对端数据时调用）。
	 */
	void touch() {
		lastActiveNanos = System.nanoTime();
	}

	long getLastActiveNanos() {
		return lastActiveNanos;
	}

	boolean isClosed() {
		return closed.get();
	}

	/**
	 * 将业务任务提交到本 peer 的串行队列，由 {@code pool} 调度执行。
	 *
	 * @param pool 业务线程池
	 * @param task 任务
	 */
	void executeSerially(ExecutorService pool, Runnable task) {
		serialTasks.offer(task);
		trySchedule(pool);
	}

	private void trySchedule(ExecutorService pool) {
		if (!serialScheduled.compareAndSet(false, true)) {
			return;
		}
		try {
			pool.execute(() -> drainSerial(pool));
		} catch (RejectedExecutionException e) {
			serialScheduled.set(false);
			throw e;
		}
	}

	private void drainSerial(ExecutorService pool) {
		try {
			Runnable task;
			while ((task = serialTasks.poll()) != null) {
				task.run();
			}
		} finally {
			serialScheduled.set(false);
			if (!serialTasks.isEmpty()) {
				try {
					trySchedule(pool);
				} catch (RejectedExecutionException e) {
					// 池已关闭或饱和：剩余任务随会话生命周期丢弃
				}
			}
		}
	}

	@Override
	public boolean send(Packet packet) {
		if (!running.get() || closed.get()) {
			return false;
		}
		if (packet == null) {
			return false;
		}
		ByteBuffer encoded = handler.encode(packet, config, this);
		if (encoded == null) {
			return false;
		}
		boolean offered = sendOffer.offer(new OutboundDatagram(encoded, remote));
		if (!offered) {
			droppedSendCount.incrementAndGet();
		}
		return offered;
	}

	@Override
	public void close(String remark) {
		if (!closed.compareAndSet(false, true)) {
			return;
		}
		onClosed.accept(remote);
	}

	/**
	 * 累计因发送队列满等原因丢弃的回包数。
	 *
	 * @return 累计丢弃次数
	 */
	public long getDroppedSendCount() {
		return droppedSendCount.get();
	}

	void incrementDroppedSend() {
		droppedSendCount.incrementAndGet();
	}

	/**
	 * 待发送报文：编码后的缓冲 + 目标地址。
	 */
	static final class OutboundDatagram {
		final ByteBuffer buffer;
		final InetSocketAddress remote;

		OutboundDatagram(ByteBuffer buffer, InetSocketAddress remote) {
			this.buffer = buffer;
			this.remote = remote;
		}
	}

	/**
	 * 发送入队。
	 */
	@FunctionalInterface
	interface SendOffer {
		boolean offer(OutboundDatagram datagram);
	}
}
