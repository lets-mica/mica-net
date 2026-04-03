/*
	Apache License
	Version 2.0, January 2004
	http://www.apache.org/licenses/
*/
package net.dreamlu.mica.net.server.udp;

import net.dreamlu.mica.net.server.TioServerConfig;
import net.dreamlu.mica.net.server.intf.TioServerHandler;
import net.dreamlu.mica.net.server.intf.TioServerListener;
import net.dreamlu.mica.net.utils.thread.pool.SynThreadPoolExecutor;

import java.util.concurrent.ExecutorService;

/**
 * UDP Server Configuration
 * This is a specialized configuration for UDP servers that extends TioServerConfig
 * but excludes TCP-specific features like backlog, accept handler, and proxy protocol.
 * <p>
 * Note: UDP is connectionless, so TCP-specific features like backlog, proxy protocol,
 * and heartbeat backoff are not applicable and not exposed in this configuration.
 *
 * @author L.cm
 */
public class UdpServerConfig extends TioServerConfig {

	/**
	 * Create UDP server configuration
	 *
	 * @param tioServerHandler  TioServerHandler
	 * @param tioServerListener TioServerListener
	 */
	public UdpServerConfig(TioServerHandler tioServerHandler, TioServerListener tioServerListener) {
		super(tioServerHandler, tioServerListener);
	}

	/**
	 * Create UDP server configuration with name
	 *
	 * @param name              name
	 * @param tioServerHandler  TioServerHandler
	 * @param tioServerListener TioServerListener
	 */
	public UdpServerConfig(String name, TioServerHandler tioServerHandler, TioServerListener tioServerListener) {
		super(name, tioServerHandler, tioServerListener);
	}

	/**
	 * Create UDP server configuration with custom executors
	 *
	 * @param tioServerHandler  TioServerHandler
	 * @param tioServerListener TioServerListener
	 * @param tioExecutor       SynThreadPoolExecutor
	 * @param groupExecutor     ThreadPoolExecutor
	 */
	public UdpServerConfig(TioServerHandler tioServerHandler, TioServerListener tioServerListener,
						   SynThreadPoolExecutor tioExecutor, java.util.concurrent.ThreadPoolExecutor groupExecutor) {
		super(tioServerHandler, tioServerListener, tioExecutor, groupExecutor);
	}

	/**
	 * Create UDP server configuration with name and custom executors
	 *
	 * @param name              name
	 * @param tioServerHandler  TioServerHandler
	 * @param tioServerListener TioServerListener
	 * @param tioExecutor       SynThreadPoolExecutor
	 * @param groupExecutor     ExecutorService
	 */
	public UdpServerConfig(String name, TioServerHandler tioServerHandler, TioServerListener tioServerListener,
						   SynThreadPoolExecutor tioExecutor, ExecutorService groupExecutor) {
		super(name, tioServerHandler, tioServerListener, tioExecutor, groupExecutor);
	}

	@Override
	public String toString() {
		return "UdpServerConfig [name=" + name + "]";
	}
}
