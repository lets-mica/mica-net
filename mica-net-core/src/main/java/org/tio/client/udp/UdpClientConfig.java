/*
	Apache License
	Version 2.0, January 2004
	http://www.apache.org/licenses/
*/
package org.tio.client.udp;

import org.tio.client.TioClientConfig;
import org.tio.client.intf.TioClientHandler;
import org.tio.client.intf.TioClientListener;
import org.tio.utils.thread.pool.SynThreadPoolExecutor;

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

	/**
	 * Create UDP client configuration without reconnection support
	 * UDP is connectionless, so reconnection doesn't make sense
	 *
	 * @param tioHandler  TioClientHandler
	 * @param tioListener TioClientListener
	 */
	public UdpClientConfig(TioClientHandler tioHandler, TioClientListener tioListener) {
		// UDP doesn't need reconnection, so pass null
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
		// UDP doesn't need reconnection, so pass null
		super(tioHandler, tioListener, null, tioExecutor, groupExecutor);
	}

	@Override
	public String toString() {
		return "UdpClientConfig [name=" + name + "]";
	}
}
