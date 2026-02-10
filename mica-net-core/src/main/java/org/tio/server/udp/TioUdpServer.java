package org.tio.server.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.ChannelContext;
import org.tio.core.Node;
import org.tio.core.udp.UdpChannelContext;
import org.tio.server.TioServerConfig;

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
public class TioUdpServer implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(TioUdpServer.class);
	private final TioServerConfig tioServerConfig;
	private final int port;
	private DatagramChannel datagramChannel;
	private Selector selector;
	private Thread selectorThread;
	private volatile boolean isStopped = false;
	private final ByteBuffer readBuffer;

	public TioUdpServer(TioServerConfig tioServerConfig, int port) {
		this.tioServerConfig = tioServerConfig;
		this.port = port;
		this.readBuffer = ByteBuffer.allocate(tioServerConfig.getReadBufferSize());
	}

	public void start() throws IOException {
		selector = Selector.open();
		datagramChannel = DatagramChannel.open();
		datagramChannel.configureBlocking(false);
		datagramChannel.socket().bind(new InetSocketAddress(port));
		datagramChannel.register(selector, SelectionKey.OP_READ);

		selectorThread = new Thread(this, "tio-udp-server-" + port);
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

			ChannelContext channelContext = tioServerConfig.clientNodes.find(remoteNode);
			if (channelContext == null) {
				channelContext = new UdpChannelContext(tioServerConfig, channel, remoteNode);
				tioServerConfig.clientNodes.put(channelContext);
			}

			// Copy data to a new buffer because readBuffer is reused
			ByteBuffer newBuffer = ByteBuffer.allocate(readBuffer.remaining());
			newBuffer.put(readBuffer);
			newBuffer.flip();

			if (tioServerConfig.useQueueDecode) {
				channelContext.decodeRunnable.addMsg(newBuffer);
				channelContext.decodeRunnable.execute();
			} else {
				channelContext.decodeRunnable.setNewReceivedByteBuffer(newBuffer);
				channelContext.decodeRunnable.decode();
			}

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
