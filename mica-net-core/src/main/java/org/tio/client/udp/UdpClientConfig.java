/*
	Apache License
	Version 2.0, January 2004
	http://www.apache.org/licenses/
*/
package org.tio.client.udp;

import org.tio.client.TioClientConfig;
import org.tio.client.intf.TioClientHandler;
import org.tio.client.intf.TioClientListener;
import org.tio.core.Node;
import org.tio.utils.thread.pool.SynThreadPoolExecutor;

import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ExecutorService;

/**
 * UDP Client Configuration
 * This is a specialized configuration for UDP clients that extends TioClientConfig
 * but excludes TCP-specific features like reconnection and connection completion handlers.
 * <p>
 * Note: UDP is connectionless, so features like reconnection and heartbeat timeout
 * are not applicable and not exposed in this configuration.
 *
 * @author L.cm
 */
public class UdpClientConfig extends TioClientConfig {
	private TioUdpClient udpClient;

	/**
	 * Create UDP client configuration without reconnection support
	 * UDP is connectionless, so reconnection doesn't make sense
	 *
	 * @param tioHandler  TioClientHandler
	 * @param tioListener TioClientListener
	 */
	public UdpClientConfig(TioClientHandler tioHandler, TioClientListener tioListener) {
		super(tioHandler, tioListener, null);
	}

	/**
	 * Create UDP client configuration with custom executors
	 *
	 * @param tioHandler    TioClientHandler
	 * @param tioListener   TioClientListener
	 * @param tioExecutor   SynThreadPoolExecutor
	 * @param groupExecutor ExecutorService
	 */
	public UdpClientConfig(TioClientHandler tioHandler, TioClientListener tioListener,
						   SynThreadPoolExecutor tioExecutor, ExecutorService groupExecutor) {
		super(tioHandler, tioListener, null, tioExecutor, groupExecutor);
	}

	/**
	 * UDP connect
	 *
	 * @param serverNode serverNode
	 * @param timeout    timeout
	 * @return UdpClientChannelContext
	 * @throws Exception Exception
	 */
	public UdpClientChannelContext connect(Node serverNode, Integer timeout) throws Exception {
		return connect(serverNode, null, null, timeout);
	}

	/**
	 * UDP connect
	 *
	 * @param serverNode serverNode
	 * @param bindIp     bindIp
	 * @param bindPort   bindPort
	 * @param timeout    timeout
	 * @return UdpClientChannelContext
	 * @throws Exception Exception
	 */
	public synchronized UdpClientChannelContext connect(Node serverNode, String bindIp, Integer bindPort, Integer timeout) throws Exception {
		if (udpClient == null) {
			udpClient = new TioUdpClient();
		}

		DatagramChannel datagramChannel = DatagramChannel.open();
		datagramChannel.configureBlocking(false);

		InetSocketAddress bindAdder;
		if (bindPort != null) {
			bindAdder = (bindIp == null) ? new InetSocketAddress(bindPort) : new InetSocketAddress(bindIp, bindPort);
		} else {
			bindAdder = new InetSocketAddress(0);
		}
		datagramChannel.bind(bindAdder);

		InetSocketAddress remote = new InetSocketAddress(serverNode.getIp(), serverNode.getPort());
		datagramChannel.connect(remote);

		UdpClientChannelContext context = new UdpClientChannelContext(this, datagramChannel);
		context.setServerNode(serverNode);

		udpClient.register(datagramChannel, context);
		connecteds.add(context);
		return context;
	}

	/**
	 * Stop the UDP client
	 */
	public void stopUdpClient() {
		if (udpClient != null) {
			udpClient.stop();
		}
	}

	@Override
	public String toString() {
		return "UdpClientConfig [name=" + name + "]";
	}
}
