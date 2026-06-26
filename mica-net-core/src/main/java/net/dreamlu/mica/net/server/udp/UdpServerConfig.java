/*
	Apache License
	Version 2.0, January 2004
	http://www.apache.org/licenses/
*/
package net.dreamlu.mica.net.server.udp;

import net.dreamlu.mica.net.core.udp.UdpConfig;

/**
 * UDP 服务端配置。
 * <p>
 * 用法：
 * <pre>{@code
 * UdpServerConfig cfg = UdpServerConfig.builder()
 *     .port(9999)
 *     .readBufferSize(4096)
 *     .workerThreads(8)
 *     .workerPool(myExecutor)   // 可选：不传则使用默认池
 *     .build();
 * UdpServer server = new UdpServer(cfg, handler);
 * }</pre>
 *
 * @author L.cm
 */
public final class UdpServerConfig extends UdpConfig {
	private final int port;
	private final int workerThreads;

	private UdpServerConfig(Builder builder) {
		super(builder);
		this.port = builder.port;
		this.workerThreads = builder.workerThreads;
	}

	public int getPort() {
		return port;
	}

	public int getWorkerThreads() {
		return workerThreads;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder extends UdpConfig.Builder<Builder> {
		private int port;
		private int workerThreads = Math.max(2, Runtime.getRuntime().availableProcessors() * 2);

		private Builder() {
		}

		public Builder port(int port) {
			this.port = port;
			return this;
		}

		public Builder workerThreads(int workerThreads) {
			if (workerThreads <= 0) {
				throw new IllegalArgumentException("workerThreads must be > 0");
			}
			this.workerThreads = workerThreads;
			return this;
		}

		@Override
		public UdpServerConfig build() {
			if (port <= 0 || port > 65535) {
				throw new IllegalStateException("port must be in (0, 65535]");
			}
			return new UdpServerConfig(this);
		}
	}
}
