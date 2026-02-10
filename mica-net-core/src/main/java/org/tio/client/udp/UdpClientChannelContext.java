package org.tio.client.udp;

import org.tio.core.Node;
import org.tio.core.TioConfig;
import org.tio.core.udp.UdpChannelContext;

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
        // We pass null for remoteNode to super because for a client, the remote node is the ServerNode,
        // not the ClientNode. We set them manually below.
        super(tioConfig, datagramChannel, null);

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
