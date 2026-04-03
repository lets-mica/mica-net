/*
	Apache License
	Version 2.0, January 2004
	http://www.apache.org/licenses/
*/
package net.dreamlu.mica.net.client.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * NIO UDP Client Selector Loop
 *
 * @author L.cm
 */
public class UdpClient implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(UdpClient.class);
	private final Selector selector;
	private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
	private volatile boolean stopped = false;

	public UdpClient() throws IOException {
		this.selector = Selector.open();
		Thread thread = new Thread(this, "tio-udp-client-selector");
		thread.setDaemon(true);
		thread.start();
	}

	public void register(DatagramChannel channel, UdpClientChannelContext context) {
		taskQueue.add(() -> {
			try {
				channel.register(selector, SelectionKey.OP_READ, context);
			} catch (Exception e) {
				log.error("Register UDP channel error", e);
			}
		});
		selector.wakeup();
	}

	@Override
	public void run() {
		while (!stopped && !Thread.currentThread().isInterrupted()) {
			try {
				// Process pending tasks
				Runnable task;
				while ((task = taskQueue.poll()) != null) {
					try {
						task.run();
					} catch (Throwable e) {
						log.error(e.getMessage(), e);
					}
				}

				int readyChannels = selector.select(1000);
				if (readyChannels == 0) continue;

				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

				while (keyIterator.hasNext()) {
					SelectionKey key = keyIterator.next();
					keyIterator.remove();

					if (!key.isValid()) {
						continue;
					}

					if (key.isReadable()) {
						handleRead(key);
					}
				}

			} catch (Throwable e) {
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
	}

	private void handleRead(SelectionKey key) {
		UdpClientChannelContext context = (UdpClientChannelContext) key.attachment();
		DatagramChannel channel = (DatagramChannel) key.channel();

		try {
			// Allocate buffer based on config
			ByteBuffer buffer = ByteBuffer.allocate(context.getReadBufferSize());
			// Since we connected the channel, we can use read()
			int read = channel.read(buffer);
			if (read > 0) {
				buffer.flip();
				// Use the unified method from UdpChannelContext
				context.handleReceivedData(buffer);
			} else if (read < 0) {
				context.handleReadError(null, "UDP read returned -1");
			}
		} catch (Throwable e) {
			context.handleReadError(e, "UDP Read error");
		}
	}

	public void stop() {
		stopped = true;
		if (selector != null) {
			selector.wakeup();
		}
	}
}
