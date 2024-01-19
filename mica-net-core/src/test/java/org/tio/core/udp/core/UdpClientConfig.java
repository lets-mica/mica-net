package org.tio.core.udp.core;

import org.tio.core.Node;
import org.tio.core.TioConfig;
import org.tio.core.intf.TioHandler;
import org.tio.core.intf.TioListener;
import org.tio.core.udp.core.intf.UdpHandler;
import org.tio.core.udp.core.intf.UdpListener;

/**
 * udp 客户端配置
 *
 * @author L.cm
 */
public class UdpClientConfig extends TioConfig {
	private int timeout = 5000;
	private Node serverNode;
	private final UdpHandler udpHandler;
	private final UdpListener udpListener;

	public UdpClientConfig(UdpHandler udpHandler, UdpListener udpListener) {
		super();
		this.udpHandler = udpHandler;
		this.udpListener = udpListener;
	}

	@Override
	public TioHandler getTioHandler() {
		return this.udpHandler;
	}

	@Override
	public TioListener getTioListener() {
		return this.udpListener;
	}

	@Override
	public boolean isServer() {
		return false;
	}
}
