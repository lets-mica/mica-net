/*
	Apache License
	Version 2.0, January 2004
	http://www.apache.org/licenses/
*/
package net.dreamlu.mica.net.client.udp;

import net.dreamlu.mica.net.core.udp.UdpConfig;

/**
 * UDP 客户端配置。
 * <p>
 * 用法：
 * <pre>{@code
 * UdpClientConfig cfg = UdpClientConfig.builder()
 *     .host("127.0.0.1")
 *     .port(9999)
 *     .readBufferSize(4096)
 *     .build();
 * UdpClient client = new UdpClient(cfg, handler);
 * }</pre>
 *
 * @author L.cm
 */
public final class UdpClientConfig extends UdpConfig {
	private final String host;
	private final int port;

	private UdpClientConfig(Builder builder) {
		super(builder);
		this.host = builder.host;
		this.port = builder.port;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder extends UdpConfig.Builder<Builder> {
		private String host = "127.0.0.1";
		private int port;

		private Builder() {
		}

		public Builder host(String host) {
			this.host = host;
			return this;
		}

		public Builder port(int port) {
			this.port = port;
			return this;
		}

		@Override
		public UdpClientConfig build() {
			if (host == null || host.isEmpty()) {
				throw new IllegalStateException("host must not be empty");
			}
			if (port <= 0 || port > 65535) {
				throw new IllegalStateException("port must be in (0, 65535]");
			}
			return new UdpClientConfig(this);
		}
	}
}
