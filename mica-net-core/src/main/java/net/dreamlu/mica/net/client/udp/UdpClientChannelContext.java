package net.dreamlu.mica.net.client.udp;

import net.dreamlu.mica.net.core.Node;
import net.dreamlu.mica.net.core.TioConfig;
import net.dreamlu.mica.net.core.udp.UdpChannelContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * UDP Client Channel Context
 */
public class UdpClientChannelContext extends UdpChannelContext {
    private final AtomicInteger reConnCount = new AtomicInteger();
    private String bindIp;
    private Integer bindPort;

    public UdpClientChannelContext(TioConfig tioConfig, DatagramChannel datagramChannel) {
        // Use the protected constructor that doesn't require remoteNode
        super(tioConfig, datagramChannel);

        // Initialize client node from local address
        initializeClientNode(datagramChannel);
    }

    /**
     * Initialize client node from DatagramChannel's local address
     */
    private void initializeClientNode(DatagramChannel datagramChannel) {
        try {
            if (datagramChannel.getLocalAddress() != null) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress) datagramChannel.getLocalAddress();
                this.setClientNode(new Node(inetSocketAddress.getHostString(), inetSocketAddress.getPort()));
            } else {
                assignAnUnknownClientNode();
            }
        } catch (IOException e) {
            assignAnUnknownClientNode();
        }
    }

    public AtomicInteger getReConnCount() {
        return reConnCount;
    }

    public String getBindIp() {
        return bindIp;
    }

    public void setBindIp(String bindIp) {
        this.bindIp = bindIp;
    }

    public Integer getBindPort() {
        return bindPort;
    }

    public void setBindPort(Integer bindPort) {
        this.bindPort = bindPort;
    }

    @Override
    public boolean isServer() {
        return false;
    }
}
