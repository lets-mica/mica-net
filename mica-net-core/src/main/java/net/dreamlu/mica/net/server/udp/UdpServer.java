package net.dreamlu.mica.net.server.udp;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.Node;
import net.dreamlu.mica.net.core.udp.UdpChannelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

/**
 * NIO UDP Server implementation.
 * Replaces the old BIO UdpServer.
 */
public class UdpServer implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(UdpServer.class);
	private final UdpServerConfig serverConfig;
	private final int port;
	private final ByteBuffer readBuffer;
	private DatagramChannel datagramChannel;
	private Selector selector;
	private volatile boolean isStopped = false;

	public UdpServer(UdpServerConfig serverConfig, int port) {
		this.serverConfig = serverConfig;
		this.port = port;
		this.readBuffer = ByteBuffer.allocate(serverConfig.getReadBufferSize());
	}

	public void start() throws IOException {
		selector = Selector.open();
		datagramChannel = DatagramChannel.open();
		datagramChannel.configureBlocking(false);
		datagramChannel.socket().bind(new InetSocketAddress(port));
		datagramChannel.register(selector, SelectionKey.OP_READ);
		Thread selectorThread = new Thread(this, "tio-udp-server-" + port);
		selectorThread.setDaemon(false);
		selectorThread.start();
		log.info("NIO UDP Server started on port {}", port);
	}

	@Override
	public void run() {
		while (!isStopped) {
			try {
				if (selector.select() > 0) {
					Set<SelectionKey> selectedKeys = selector.selectedKeys();
					Iterator<SelectionKey> iterator = selectedKeys.iterator();
					while (iterator.hasNext()) {
						SelectionKey key = iterator.next();
						iterator.remove();
						if (key.isReadable()) {
							handleRead((DatagramChannel) key.channel());
						}
					}
				}
			} catch (Throwable e) {
				log.error("NIO UDP Server select error", e);
			}
		}
	}

	private void handleRead(DatagramChannel channel) {
		try {
			readBuffer.clear();
			SocketAddress remoteAddress = channel.receive(readBuffer);
			if (remoteAddress == null) {
				return;
			}
			readBuffer.flip();

			InetSocketAddress inetSocketAddress = (InetSocketAddress) remoteAddress;
			Node remoteNode = new Node(inetSocketAddress.getHostString(), inetSocketAddress.getPort());

			ChannelContext channelContext = serverConfig.clientNodes.find(remoteNode);
			if (channelContext == null) {
				channelContext = new UdpChannelContext(serverConfig, channel, remoteNode);
				serverConfig.clientNodes.put(channelContext);
			}

			// Copy data to a new buffer because readBuffer is reused
			ByteBuffer newBuffer = ByteBuffer.allocate(readBuffer.remaining());
			newBuffer.put(readBuffer);
			newBuffer.flip();

			// Use the unified method from UdpChannelContext
			((UdpChannelContext) channelContext).handleReceivedData(newBuffer);
		} catch (Throwable e) {
			log.error("NIO UDP handle read error", e);
		}
	}

	public void stop() {
		isStopped = true;
		if (selector != null) {
			selector.wakeup();
			try {
				selector.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
		if (datagramChannel != null) {
			try {
				datagramChannel.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}
}
