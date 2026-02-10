package org.tio.core.udp;

import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.core.TioConfig;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.EncodedPacket;
import org.tio.core.intf.Packet;
import org.tio.server.DefaultTioServerListener;
import org.tio.server.TioServerConfig;
import org.tio.server.intf.TioServerHandler;
import org.tio.server.udp.TioUdpServer;
import org.tio.server.udp.UdpServerConfig;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * UDP Server Usage Example
 */
public class UdpServerDemo {

	public static void main(String[] args) throws Exception {
		// 1. Define Handler
		TioServerHandler serverHandler = new UdpServerHandler();

		// 2. Configure Server
		UdpServerConfig serverConfig = new UdpServerConfig(serverHandler, new DefaultTioServerListener());
		serverConfig.setName("UdpServerDemo");

		// 3. Initialize and Start NIO UDP Server
		// Note: Using NioUdpServer directly for UDP
		TioUdpServer udpServer = new TioUdpServer(serverConfig, 3000);
		udpServer.start();

		// Keep the main thread alive
		// System.in.read();
	}

	public static class UdpServerHandler implements TioServerHandler {
		@Override
		public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext) throws TioDecodeException {
			// Simple decoder: read all available bytes
			byte[] bytes = new byte[readableLength];
			buffer.get(bytes);
			return new EncodedPacket(bytes);
		}

		@Override
		public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
			// Simple encoder: wrap bytes
			return ByteBuffer.wrap(((EncodedPacket) packet).getBytes());
		}

		@Override
		public void handler(Packet packet, ChannelContext channelContext) throws Exception {
			String msg = new String(((EncodedPacket) packet).getBytes(), StandardCharsets.UTF_8);
			System.out.println("Server received: " + msg);

			// Echo back response
			String response = "Echo: " + msg;
			EncodedPacket responsePacket = new EncodedPacket(response.getBytes(StandardCharsets.UTF_8));
			Tio.send(channelContext, responsePacket);
		}
	}
}
