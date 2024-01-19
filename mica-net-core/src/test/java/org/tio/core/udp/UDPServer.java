package org.tio.core.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.util.Iterator;

/**
 * 调研 udp 非阻塞形式
 *
 * @author L.cm
 */
public class UDPServer {

	public static void main(String[] args) throws IOException {
		DatagramChannel channel = DatagramChannel.open();
		channel.socket().bind(new InetSocketAddress(9999));
		channel.configureBlocking(false);

		Selector selector = Selector.open();
		channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

		ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);
		ByteBuffer sendBuffer = ByteBuffer.wrap("Hello, Client!".getBytes());

		while (true) {
			selector.select();

			Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
				iterator.remove();

				if (key.isReadable()) {
					DatagramChannel datagramChannel = (DatagramChannel) key.channel();
					receiveBuffer.clear();
					InetSocketAddress clientAddress = (InetSocketAddress) datagramChannel.receive(receiveBuffer);
					receiveBuffer.flip();

					String message = new String(receiveBuffer.array(), 0, receiveBuffer.limit());
					System.out.println("收到客户端 " + clientAddress.getAddress().getHostAddress() + ":" + clientAddress.getPort() + " 消息：" + message);
				}

				if (key.isWritable()) {
					DatagramChannel datagramChannel = (DatagramChannel) key.channel();
					sendBuffer.rewind();
					InetSocketAddress clientAddress = new InetSocketAddress("localhost", 9999);
					int size = datagramChannel.send(sendBuffer, clientAddress);
					System.out.println("向客户端发送消息：" + new String(sendBuffer.array(), 0, sendBuffer.limit()));
				}
			}
		}
	}
}
