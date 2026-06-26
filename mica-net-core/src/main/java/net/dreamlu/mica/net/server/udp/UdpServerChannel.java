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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.atomic.AtomicLong;

/**
 * UDP 服务端 {@link UdpChannel} 实现。
 * <p>
 * send 同步写入 socket：与客户端不同，server 端每次回包的目标是对端独立地址，
 * 不需要单独的发送线程。
 * <p>
 * 在非阻塞模式下，{@link DatagramChannel#send} 不会阻塞；若内核 send buffer
 * 已满，JDK 抛 {@link IOException}，本类将其视为 backpressure 记一次
 * {@link #getDroppedSendCount()} 并降级到 warn 日志，避免淹没真正的错误日志。
 * <p>
 * 类是 public 但构造器是 package-private：仅 {@link UdpServer} 能创建实例，
 * 业务代码通过 {@link UdpHandler#handler} 回调拿到该对象，按需转成具体类型。
 *
 * @author L.cm
 */
public class UdpServerChannel implements UdpChannel {
	private static final Logger log = LoggerFactory.getLogger(UdpServerChannel.class);
	private final UdpServerConfig config;
	private final UdpHandler handler;
	private final DatagramChannel channel;
	private final InetSocketAddress remote;
	private final AtomicLong droppedSendCount = new AtomicLong();

	UdpServerChannel(UdpServerConfig config,
					 UdpHandler handler,
					 DatagramChannel channel,
					 InetSocketAddress remote) {
		this.config = config;
		this.handler = handler;
		this.channel = channel;
		this.remote = remote;
	}

	@Override
	public UdpServerConfig getConfig() {
		return config;
	}

	@Override
	public InetSocketAddress remoteAddress() {
		return remote;
	}

	@Override
	public boolean send(Packet packet) {
		ByteBuffer encoded = handler.encode(packet, config, this);
		if (encoded == null) {
			return false;
		}
		try {
			channel.send(encoded, remote);
			return true;
		} catch (IOException e) {
			// 非阻塞 send 在内核缓冲区满时抛 IOException，视为背压丢包。
			// 其它 IO 错误同样记录到计数器，但日志降为 warn，避免淹没真错误。
			droppedSendCount.incrementAndGet();
			log.warn("udp send dropped to {}: {}", remote, e.getMessage());
			return false;
		}
	}

	/**
	 * 累计因 send buffer 满 / 其它 IO 错误而丢弃的回包数。
	 * 用于接入监控 / 排查链路背压。
	 *
	 * @return 累计丢弃次数
	 */
	public long getDroppedSendCount() {
		return droppedSendCount.get();
	}
}
