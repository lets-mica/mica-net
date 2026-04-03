package net.dreamlu.mica.net.core.udp;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.Tio;
import net.dreamlu.mica.net.core.TioConfig;
import net.dreamlu.mica.net.core.exception.TioDecodeException;
import net.dreamlu.mica.net.core.intf.EncodedPacket;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.server.DefaultTioServerListener;
import net.dreamlu.mica.net.server.intf.TioServerHandler;
import net.dreamlu.mica.net.server.udp.UdpServer;
import net.dreamlu.mica.net.server.udp.UdpServerConfig;

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
		UdpServer udpServer = new UdpServer(serverConfig, 3000);
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
