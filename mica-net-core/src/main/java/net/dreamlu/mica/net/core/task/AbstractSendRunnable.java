/*
	Apache License
	Version 2.0, January 2004
	http://www.apache.org/licenses/

	Copyright 2020 t-io

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	  http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
package net.dreamlu.mica.net.core.task;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.ChannelContext.CloseCode;
import net.dreamlu.mica.net.core.TcpConst;
import net.dreamlu.mica.net.core.Tio;
import net.dreamlu.mica.net.core.TioConfig;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.core.intf.TcpHandler;
import net.dreamlu.mica.net.core.ssl.SslHandler;
import net.dreamlu.mica.net.core.ssl.SslUtils;
import net.dreamlu.mica.net.utils.buffer.ByteBufferUtil;
import net.dreamlu.mica.net.utils.thread.pool.AbstractQueueRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * 发送任务抽象基类 - 提取 TCP/UDP 共享逻辑
 *
 * @author tanyaowu
 */
public abstract class AbstractSendRunnable extends AbstractQueueRunnable<Packet> {
	private static final Logger log = LoggerFactory.getLogger(AbstractSendRunnable.class);
	protected static final int MAX_CAPACITY_MIN = TcpConst.MAX_DATA_LENGTH - 1024;
	protected static final int MAX_CAPACITY_MAX = MAX_CAPACITY_MIN * 10;
	protected static final int MAX_BATCH_SIZE = 512;
	private static final int INITIAL_BATCH_CAPACITY = 32;
	// 共享字段
	protected final ChannelContext channelContext;
	protected final TioConfig tioConfig;
	protected final TcpHandler tioHandler;
	protected final Queue<Packet> msgQueue;
	protected Queue<Packet> forSendAfterSslHandshakeCompleted = null;
	/**
	 * 握手完成前暂存业务包时的锁。
	 * <p>
	 * 暂存（放入）与握手完成后的排空（取出）都在这把锁内完成，
	 * 避免"放入与排空交叉"导致业务包被永久滞留：排空一定发生在握手完成之后，
	 * 而暂存时的握手状态判断与放入是原子的，两者不会错开。
	 * </p>
	 */
	private final Object sslDeferredLock = new Object();

	public AbstractSendRunnable(ChannelContext channelContext, Executor executor) {
		this(channelContext, executor, new ConcurrentLinkedQueue<>());
	}

	public AbstractSendRunnable(ChannelContext channelContext, Executor executor, Queue<Packet> msgQueue) {
		super(executor);
		this.channelContext = channelContext;
		this.tioConfig = channelContext.tioConfig;
		this.tioHandler = tioConfig.getTioHandler();
		this.msgQueue = msgQueue;
	}

	public Queue<Packet> getForSendAfterSslHandshakeCompleted(boolean forceCreate) {
		if (forSendAfterSslHandshakeCompleted == null && forceCreate) {
			synchronized (this) {
				if (forSendAfterSslHandshakeCompleted == null) {
					forSendAfterSslHandshakeCompleted = new ConcurrentLinkedQueue<>();
				}
			}
		}
		return forSendAfterSslHandshakeCompleted;
	}

	/**
	 * SSL 握手未完成时，把需要加密的业务包暂存起来，等握手完成后再发送。
	 * <p>
	 * 握手未完成时不能加密业务数据：引擎此时只会产出握手记录，业务数据不会被消费，
	 * 直接发出去对端收到的是非法 TLS 记录。
	 * </p>
	 *
	 * @param packet 业务包
	 * @return true 表示已暂存（调用方不要再走正常发送路径）；false 表示握手已完成或该包无需加密，走正常发送路径
	 */
	public boolean deferIfSslHandshakeNotCompleted(Packet packet) {
		if (!SslUtils.needSslEncrypt(packet, tioConfig)) {
			// 已加密的包或非 SSL 连接，不需要暂存
			return false;
		}
		synchronized (sslDeferredLock) {
			SslHandler sslHandler = channelContext.getSslHandler();
			if (sslHandler == null || sslHandler.isHandshakeCompleted()) {
				return false;
			}
			getForSendAfterSslHandshakeCompleted(true).add(packet);
			return true;
		}
	}

	/**
	 * 取走握手完成前暂存的业务包。
	 * <p>
	 * 与 {@link #deferIfSslHandshakeNotCompleted(Packet)} 用同一把锁，保证暂存与排空不会交叉。
	 * </p>
	 *
	 * @return 暂存的业务包列表（可能为空）
	 */
	public List<Packet> takeDeferredPackets() {
		synchronized (sslDeferredLock) {
			List<Packet> packets = new ArrayList<>(2);
			Queue<Packet> deferred = forSendAfterSslHandshakeCompleted;
			if (deferred == null) {
				return packets;
			}
			Packet packet;
			while ((packet = deferred.poll()) != null) {
				packets.add(packet);
			}
			return packets;
		}
	}

	@Override
	public boolean addMsg(Packet packet) {
		if (this.isCanceled()) {
			log.info("{}, 任务已经取消，{}添加到发送队列失败", channelContext, packet.logstr());
			return false;
		}
		if (deferIfSslHandshakeNotCompleted(packet)) {
			return true;
		}
		return msgQueue.add(packet);
	}

	@Override
	public void clearMsgQueue() {
		Packet p;
		// 先取走握手完成前暂存的包，再置空，否则暂存的包会被直接丢弃，
		// 等待该包发送完成的业务方（如 bSend）会一直等到超时
		List<Packet> deferred = takeDeferredPackets();
		forSendAfterSslHandshakeCompleted = null;
		while ((p = msgQueue.poll()) != null) {
			try {
				channelContext.processAfterSent(p, false);
			} catch (Throwable e) {
				log.error(e.getMessage(), e);
			}
		}
		for (Packet packet : deferred) {
			try {
				channelContext.processAfterSent(packet, false);
			} catch (Throwable e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * 编码数据包
	 */
	protected ByteBuffer getByteBuffer(Packet packet) {
		try {
			ByteBuffer byteBuffer = packet.getPreEncodedByteBuffer();
			if (byteBuffer == null) {
				byteBuffer = tioHandler.encode(packet, tioConfig, channelContext);
			}
			if (!byteBuffer.hasRemaining()) {
				byteBuffer.flip();
			}
			return byteBuffer;
		} catch (Exception e) {
			log.error(packet.logstr(), e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * SSL 加密处理
	 */
	protected ByteBuffer encryptIfNeeded(ByteBuffer byteBuffer, Packet packet, boolean isSsl) {
		if (isSsl && !packet.isSslEncrypted()) {
			try {
				return channelContext.getSslHandler().encrypt(byteBuffer);
			} catch (SSLException e) {
				log.error("{}, 进行SSL加密时发生了异常", channelContext, e);
				Tio.close(channelContext, "进行SSL加密时发生了异常", CloseCode.SSL_ENCRYPTION_ERROR);
				return null;
			}
		}
		return byteBuffer;
	}

	/**
	 * 批量 SSL 加密（SSL 需要合并 ByteBuffer，无 SSL 时保持数组以支持 gather write）
	 */
	protected ByteBuffer[] encryptBatchIfNeeded(ByteBuffer[] byteBuffers, boolean isSsl, boolean needSslEncrypted) {
		if (isSsl && needSslEncrypted) {
			// SSL 加密需要合并为单个 ByteBuffer
			ByteBuffer mergedBuffer = ByteBufferUtil.combine(byteBuffers);
			try {
				return new ByteBuffer[]{channelContext.getSslHandler().encrypt(mergedBuffer)};
			} catch (SSLException e) {
				log.error("{}, 进行SSL加密时发生了异常", channelContext, e);
				Tio.close(channelContext, "进行SSL加密时发生了异常", CloseCode.SSL_ENCRYPTION_ERROR);
				return null;
			}
		}
		return byteBuffers;
	}

	/**
	 * 批量收集数据包并编码（TCP 使用 ByteBuffer[] gather write）
	 *
	 * @param firstPacket 已从队列取出的首个数据包
	 * @param isSsl       是否启用 SSL
	 * @return 批量编码结果
	 */
	protected BatchEncodeResult batchEncode(Packet firstPacket, boolean isSsl) {
		Packet packet = firstPacket;
		List<Packet> packets = new ArrayList<>(INITIAL_BATCH_CAPACITY);
		List<ByteBuffer> byteBuffers = new ArrayList<>(INITIAL_BATCH_CAPACITY);
		int allBytebufferCapacity = 0;
		boolean sslEncrypted = isSsl && firstPacket != null && firstPacket.isSslEncrypted();
		boolean needSslEncrypted = isSsl && !sslEncrypted;

		while (packet != null) {
			ByteBuffer byteBuffer = getByteBuffer(packet);

			packets.add(packet);
			byteBuffers.add(byteBuffer);
			allBytebufferCapacity += byteBuffer.remaining();

			if (packets.size() >= MAX_BATCH_SIZE || allBytebufferCapacity >= MAX_CAPACITY_MAX) {
				break;
			}

			if (isSsl) {
				// SSL 批次只能包含相同加密状态的数据包，避免明文漏加密或密文被二次加密。
				Packet nextPacket = msgQueue.peek();
				if (nextPacket == null || sslEncrypted != nextPacket.isSslEncrypted()) {
					break;
				}
			}
			packet = msgQueue.poll();
		}

		if (allBytebufferCapacity == 0) {
			return null;
		}

		// 返回 ByteBuffer 数组，子类决定是使用 gather write（TCP）还是合并（UDP）
		ByteBuffer[] bufferArray = byteBuffers.toArray(new ByteBuffer[0]);
		return new BatchEncodeResult(bufferArray, packets, needSslEncrypted);
	}

	@Override
	public Queue<Packet> getMsgQueue() {
		return msgQueue;
	}

	/**
	 * 是否有写操作正在进行。
	 * <p>
	 * 默认没有进行中的写操作，TCP 实现会返回真实的写状态。
	 * </p>
	 *
	 * @return true 表示正在写
	 */
	public boolean isWriting() {
		return false;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ':' + channelContext.toString();
	}

	@Override
	public String logstr() {
		return toString();
	}

	/**
	 * 发送单个数据包（子类实现）
	 */
	public abstract boolean sendPacket(Packet packet);

	/**
	 * 发送单个数据包（带 SSL 参数，子类实现）
	 *
	 * @param packet 数据包
	 * @param isSsl  是否启用 SSL
	 * @return 发送结果
	 */
	public abstract boolean sendPacket(Packet packet, boolean isSsl);

	/**
	 * 发送 ByteBuffer（子类实现）
	 */
	protected abstract void sendByteBuffer(ByteBuffer byteBuffer, Object packets);

	/**
	 * 批量编码结果
	 */
	protected static class BatchEncodeResult {
		public final ByteBuffer[] byteBuffers;
		public final List<Packet> packets;
		public final boolean needSslEncrypted;

		public BatchEncodeResult(ByteBuffer[] byteBuffers, List<Packet> packets, boolean needSslEncrypted) {
			this.byteBuffers = byteBuffers;
			this.packets = packets;
			this.needSslEncrypted = needSslEncrypted;
		}
	}
}
