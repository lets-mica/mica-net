package org.tio.core.udp;

import org.tio.client.ClientChannelContext;
import org.tio.client.DefaultTioClientListener;
import org.tio.client.ReconnConf;
import org.tio.client.TioClient;
import org.tio.client.TioClientConfig;
import org.tio.client.intf.TioClientHandler;
import org.tio.core.ChannelContext;
import org.tio.core.Node;
import org.tio.core.Tio;
import org.tio.core.TioConfig;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.EncodedPacket;
import org.tio.core.intf.Packet;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * UDP Client Usage Example
 */
public class UdpClientDemo {

    public static void main(String[] args) throws Exception {
        // 1. Define Handler
        TioClientHandler clientHandler = new UdpClientHandler();
        
        // 2. Configure Client
        TioClientConfig clientConfig = new TioClientConfig(clientHandler, new DefaultTioClientListener());
        clientConfig.setReconnConf(new ReconnConf(0)); // Disable reconnection for this demo

        // 3. Initialize TioClient
        TioClient tioClient = new TioClient(clientConfig);
        
        // 4. Connect to UDP Server (NIO)
        // Note: Using udpConnect method
        Node serverNode = new Node("127.0.0.1", 3000);
        ClientChannelContext context = tioClient.udpConnect(serverNode, 5000);

        // 5. Send a message
        String msg = "Hello UDP World";
        EncodedPacket packet = new EncodedPacket(msg.getBytes(StandardCharsets.UTF_8));
        Tio.send(context, packet);
        System.out.println("Client sent: " + msg);
        
        // 6. Wait to receive echo
        Thread.sleep(2000);
        
        // 7. Close
        tioClient.stop();
        System.exit(0);
    }

    public static class UdpClientHandler implements TioClientHandler {
        @Override
        public Packet heartbeatPacket(ChannelContext channelContext) {
            return null;
        }

        @Override
        public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext) throws TioDecodeException {
            byte[] bytes = new byte[readableLength];
            buffer.get(bytes);
            return new EncodedPacket(bytes);
        }

        @Override
        public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
            return ByteBuffer.wrap(((EncodedPacket) packet).getBytes());
        }

        @Override
        public void handler(Packet packet, ChannelContext channelContext) throws Exception {
            String msg = new String(((EncodedPacket) packet).getBytes(), StandardCharsets.UTF_8);
            System.out.println("Client received: " + msg);
        }
    }
}
