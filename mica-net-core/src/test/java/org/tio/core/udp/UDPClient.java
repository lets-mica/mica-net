package org.tio.core.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class UDPClient {
    public static void main(String[] args) throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        String message = "Hello, Server!";
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        channel.send(buffer, new InetSocketAddress("localhost", 9999));
        channel.close();
    }
}
