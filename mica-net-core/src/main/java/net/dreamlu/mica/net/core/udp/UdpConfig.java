/*
	Apache License
	Version 2.0, January 2004
	http://www.apache.org/licenses/
*/
package net.dreamlu.mica.net.core.udp;

import net.dreamlu.mica.net.core.intf.UdpHandler;

import java.util.concurrent.ExecutorService;

/**
 * UDP 配置抽象基类，承载 server / client 共享参数。
 * <p>
 * 协议处理（{@link UdpHandler}）属于实现层，不放进配置里；本类只描述 I/O 层参数。
 *
 * @author L.cm
 */
public abstract class UdpConfig {
	protected final int readBufferSize;
	protected final ExecutorService workerPool;

	protected UdpConfig(Builder<?> builder) {
		this.readBufferSize = builder.readBufferSize;
		this.workerPool = builder.workerPool;
	}

	/**
	 * @return 单次 UDP 读取的字节缓冲大小
	 */
	public int getReadBufferSize() {
		return readBufferSize;
	}

	/**
	 * @return 业务线程池；为 {@code null} 时由 server / client 内部按默认参数创建。
	 *         若用户注入，则生命周期归用户所有，server / client 仅复用，不会 shutdown。
	 */
	public ExecutorService getWorkerPool() {
		return workerPool;
	}

	/**
	 * 通用 Builder：仅承载共享字段，子类通过 {@code builder()} 提供扩展方法。
	 */
	public static abstract class Builder<T extends Builder<T>> {
		protected int readBufferSize = 2048;
		protected ExecutorService workerPool = null;

		@SuppressWarnings("unchecked")
		protected T self() {
			return (T) this;
		}

		public T readBufferSize(int readBufferSize) {
			this.readBufferSize = readBufferSize;
			return self();
		}

		/**
		 * 注入业务线程池。
		 * <p>
		 * 典型场景：与项目内其它组件共用一个池，或复用 mica-net 的
		 * {@code ThreadUtils.getGroupExecutor()}。
		 * <p>
		 * 注意：注入的池生命周期归调用方，server / client 关闭时不会自动 shutdown。
		 * 不传则由 server / client 按各自默认参数创建并自行 shutdown。
		 *
		 * @param workerPool 业务线程池
		 * @return Builder
		 */
		public T workerPool(ExecutorService workerPool) {
			this.workerPool = workerPool;
			return self();
		}

		public abstract UdpConfig build();
	}
}
