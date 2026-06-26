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

/**
 * UDP 服务端 {@link UdpChannel} 实现。
 * <p>
 * send 同步写入 socket：与客户端不同，server 端每次回包的目标是对端独立地址，
 * 不需要单独的发送线程。
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
		ByteBuffer encoded = handler.encode(packet, config,this);
		if (encoded == null) {
			return false;
		}
		try {
			channel.send(encoded, remote);
			return true;
		} catch (IOException e) {
			log.error("udp server send error to {}", remote, e);
			return false;
		}
	}
}
