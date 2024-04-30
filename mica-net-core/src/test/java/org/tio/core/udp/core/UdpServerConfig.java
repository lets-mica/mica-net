package org.tio.core.udp.core;

import org.tio.core.TioConfig;
import org.tio.core.intf.TioHandler;
import org.tio.core.intf.TioListener;
import org.tio.core.stat.vo.StatVo;
import org.tio.core.udp.core.intf.UdpHandler;
import org.tio.core.udp.core.intf.UdpListener;

/**
 * udp 服务端配置
 *
 * @author L.cm
 */
public class UdpServerConfig extends TioConfig {
	private final UdpHandler udpHandler;
	private final UdpListener udpListener;

	public UdpServerConfig(UdpHandler udpHandler, UdpListener udpListener) {
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
		return true;
	}

	@Override
	public StatVo getStat() {
		return null;
	}
}
