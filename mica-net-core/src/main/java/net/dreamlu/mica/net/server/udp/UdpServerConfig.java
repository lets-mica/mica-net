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
 *     .peerIdleTimeoutMs(300_000)
 *     .maxPeers(10_000)
 *     .sendQueueCapacity(4096)
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
	private final long peerIdleTimeoutMs;
	private final int maxPeers;
	private final int sendQueueCapacity;

	private UdpServerConfig(Builder builder) {
		super(builder);
		this.port = builder.port;
		this.workerThreads = builder.workerThreads;
		this.peerIdleTimeoutMs = builder.peerIdleTimeoutMs;
		this.maxPeers = builder.maxPeers;
		this.sendQueueCapacity = builder.sendQueueCapacity;
	}

	public int getPort() {
		return port;
	}

	public int getWorkerThreads() {
		return workerThreads;
	}

	/**
	 * 对端会话空闲超时（毫秒）。超时后从 {@code peerChannels} 淘汰。
	 * {@code 0} 表示不按空闲淘汰（仍会在 {@code close()} 时清空）。
	 *
	 * @return 空闲超时毫秒
	 */
	public long getPeerIdleTimeoutMs() {
		return peerIdleTimeoutMs;
	}

	/**
	 * 对端会话上限；达到上限后新对端报文会被丢弃。
	 *
	 * @return 最大 peer 数
	 */
	public int getMaxPeers() {
		return maxPeers;
	}

	/**
	 * 发送队列容量；满时 {@code send} 返回 {@code false}。
	 *
	 * @return 容量
	 */
	public int getSendQueueCapacity() {
		return sendQueueCapacity;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder extends UdpConfig.Builder<Builder> {
		private int port;
		private int workerThreads = Math.max(2, Runtime.getRuntime().availableProcessors() * 2);
		private long peerIdleTimeoutMs = 300_000L;
		private int maxPeers = 10_000;
		private int sendQueueCapacity = 4096;

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

		/**
		 * 对端会话空闲超时。{@code 0} 关闭空闲淘汰。
		 *
		 * @param peerIdleTimeoutMs 毫秒
		 * @return Builder
		 */
		public Builder peerIdleTimeoutMs(long peerIdleTimeoutMs) {
			if (peerIdleTimeoutMs < 0) {
				throw new IllegalArgumentException("peerIdleTimeoutMs must be >= 0");
			}
			this.peerIdleTimeoutMs = peerIdleTimeoutMs;
			return this;
		}

		/**
		 * 对端会话上限，必须 &gt; 0。
		 *
		 * @param maxPeers 上限
		 * @return Builder
		 */
		public Builder maxPeers(int maxPeers) {
			if (maxPeers <= 0) {
				throw new IllegalArgumentException("maxPeers must be > 0");
			}
			this.maxPeers = maxPeers;
			return this;
		}

		/**
		 * 发送队列容量，必须 &gt; 0。
		 *
		 * @param sendQueueCapacity 容量
		 * @return Builder
		 */
		public Builder sendQueueCapacity(int sendQueueCapacity) {
			if (sendQueueCapacity <= 0) {
				throw new IllegalArgumentException("sendQueueCapacity must be > 0");
			}
			this.sendQueueCapacity = sendQueueCapacity;
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
