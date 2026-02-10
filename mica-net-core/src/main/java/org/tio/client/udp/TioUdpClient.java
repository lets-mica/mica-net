/*
	Apache License
	Version 2.0, January 2004
	http://www.apache.org/licenses/
*/
package org.tio.client.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.client.ClientChannelContext;
import org.tio.core.Tio;

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
public class TioUdpClient implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(TioUdpClient.class);
	private final Selector selector;
	private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
	private final Thread thread;
	private volatile boolean stopped = false;

	public TioUdpClient() throws IOException {
		this.selector = Selector.open();
		this.thread = new Thread(this, "tio-udp-client-selector");
		this.thread.setDaemon(true);
		this.thread.start();
	}

	public void register(DatagramChannel channel, ClientChannelContext context) {
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
		ClientChannelContext context = (ClientChannelContext) key.attachment();
		DatagramChannel channel = (DatagramChannel) key.channel();

		try {
			// Allocate buffer based on config
			ByteBuffer buffer = ByteBuffer.allocate(context.getReadBufferSize());
			// Since we connected the channel, we can use read()
			int read = channel.read(buffer);
			if (read > 0) {
				buffer.flip();
				if (context.tioConfig.useQueueDecode) {
					context.decodeRunnable.addMsg(buffer);
					context.decodeRunnable.execute();
				} else {
					context.decodeRunnable.setNewReceivedByteBuffer(buffer);
					context.decodeRunnable.decode();
				}
			} else if (read < 0) {
				Tio.close(context, "UDP read returned -1", org.tio.core.ChannelContext.CloseCode.READ_ERROR);
			}
		} catch (Throwable e) {
			log.error("UDP Read error", e);
			Tio.close(context, e, "UDP Read error", org.tio.core.ChannelContext.CloseCode.READ_ERROR);
		}
	}

	public void stop() {
		stopped = true;
		if (selector != null) {
			selector.wakeup();
		}
	}
}
