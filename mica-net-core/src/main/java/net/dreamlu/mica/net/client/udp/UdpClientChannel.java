/*
	Apache License
	Version 2.0, January 2004
	http://www.apache.org/licenses/
*/
package net.dreamlu.mica.net.client.udp;

import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.core.intf.UdpChannel;
import net.dreamlu.mica.net.core.intf.UdpHandler;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UDP 客户端 {@link UdpChannel} 实现。
 * <p>
 * send 走发送队列，由 {@link UdpClient} 内部 sendLoop 异步落 socket，
 * 业务线程不会因 I/O 而阻塞。
 * <p>
 * 类是 public 但构造器是 package-private：仅 {@link UdpClient} 能创建实例，
 * 业务代码通过 {@link UdpHandler#handler} 回调拿到该对象，按需转成具体类型。
 *
 * @author L.cm
 */
public class UdpClientChannel implements UdpChannel {
	private final UdpClientConfig config;
	private final UdpHandler handler;
	private final InetSocketAddress remote;
	private final LinkedBlockingQueue<ByteBuffer> sendQueue;
	private final AtomicBoolean running;

	UdpClientChannel(UdpClientConfig config,
					 UdpHandler handler,
					 InetSocketAddress remote,
					 LinkedBlockingQueue<ByteBuffer> sendQueue,
					 AtomicBoolean running) {
		this.config = config;
		this.handler = handler;
		this.remote = remote;
		this.sendQueue = sendQueue;
		this.running = running;
	}

	@Override
	public UdpClientConfig getConfig() {
		return config;
	}

	@Override
	public InetSocketAddress remoteAddress() {
		return remote;
	}

	@Override
	public boolean send(Packet packet) {
		if (!running.get()) {
			return false;
		}
		if (packet == null) {
			return false;
		}
		ByteBuffer encoded = handler.encode(packet, config, this);
		if (encoded == null) {
			return false;
		}
		return sendQueue.offer(encoded);
	}
}
