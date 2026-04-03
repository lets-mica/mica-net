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
import net.dreamlu.mica.net.core.intf.TioHandler;
import net.dreamlu.mica.net.core.ssl.SslUtils;
import net.dreamlu.mica.net.core.ssl.SslVo;
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
	protected static final int MAX_CAPACITY_MIN = TcpConst.MAX_DATA_LENGTH - 1024;
	protected static final int MAX_CAPACITY_MAX = MAX_CAPACITY_MIN * 10;
	private static final Logger log = LoggerFactory.getLogger(AbstractSendRunnable.class);
	// 共享字段
	protected final ChannelContext channelContext;
	protected final TioConfig tioConfig;
	protected final TioHandler tioHandler;
	protected final Queue<Packet> msgQueue;
	protected Queue<Packet> forSendAfterSslHandshakeCompleted = null;

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

	/**
	 * 根据队列积压情况自适应调整批量大小
	 */
	protected static int getPacketListCapacity(int queueSize) {
		if (queueSize > 50000) {
			return 20000;
		} else if (queueSize > 20000) {
			return 8000;
		} else if (queueSize > 10000) {
			return 5000;
		} else if (queueSize > 5000) {
			return 3000;
		} else if (queueSize > 2000) {
			return 1500;
		} else if (queueSize > 1000) {
			return 1000;
		} else if (queueSize > 300) {
			return 500;
		} else {
			return queueSize;
		}
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

	@Override
	public boolean addMsg(Packet packet) {
		if (this.isCanceled()) {
			log.info("{}, 任务已经取消，{}添加到发送队列失败", channelContext, packet.logstr());
			return false;
		}
		if (channelContext.sslFacadeContext != null && !channelContext.sslFacadeContext.isHandshakeCompleted() && SslUtils.needSslEncrypt(packet, tioConfig)) {
			return this.getForSendAfterSslHandshakeCompleted(true).add(packet);
		} else {
			return msgQueue.add(packet);
		}
	}

	@Override
	public void clearMsgQueue() {
		Packet p;
		forSendAfterSslHandshakeCompleted = null;
		while ((p = msgQueue.poll()) != null) {
			try {
				channelContext.processAfterSent(p, false);
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
			SslVo sslVo = new SslVo(byteBuffer, packet);
			try {
				channelContext.sslFacadeContext.getSslFacade().encrypt(sslVo);
				return sslVo.getByteBuffer();
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
	protected ByteBuffer[] encryptBatchIfNeeded(ByteBuffer[] byteBuffers, List<Packet> packets, boolean isSsl, boolean needSslEncrypted) {
		if (isSsl && needSslEncrypted) {
			// SSL 加密需要合并为单个 ByteBuffer
			int totalCapacity = 0;
			for (ByteBuffer buffer : byteBuffers) {
				totalCapacity += buffer.remaining();
			}
			ByteBuffer mergedBuffer = ByteBuffer.allocate(totalCapacity);
			for (ByteBuffer buffer : byteBuffers) {
				mergedBuffer.put(buffer);
			}
			mergedBuffer.flip();

			SslVo sslVo = new SslVo(mergedBuffer, packets);
			try {
				channelContext.sslFacadeContext.getSslFacade().encrypt(sslVo);
				return new ByteBuffer[]{sslVo.getByteBuffer()};
			} catch (SSLException e) {
				log.error("{}, 进行SSL加密时发生了异常", channelContext, e);
				Tio.close(channelContext, "进行SSL加密时发生了异常", CloseCode.SSL_ENCRYPTION_ERROR);
				return null;
			}
		}
		return byteBuffers;
	}

	/**
	 * 批量收集数据包并编码（TCP 使用 ByteBuffer[] gather write，UDP 需要合并）
	 */
	protected BatchEncodeResult batchEncode(int queueSize, boolean isSsl) {
		int targetBatchSize = getPacketListCapacity(queueSize);

		Packet packet;
		List<Packet> packets = new ArrayList<>(targetBatchSize);
		List<ByteBuffer> byteBuffers = new ArrayList<>(targetBatchSize);
		int allBytebufferCapacity = 0;
		Boolean needSslEncrypted = null;
		boolean sslChanged = false;

		while ((packet = msgQueue.poll()) != null) {
			ByteBuffer byteBuffer = getByteBuffer(packet);

			packets.add(packet);
			byteBuffers.add(byteBuffer);
			allBytebufferCapacity += byteBuffer.limit();

			if (isSsl) {
				boolean _needSslEncrypted = !packet.isSslEncrypted();
				if (needSslEncrypted != null) {
					sslChanged = needSslEncrypted != _needSslEncrypted;
				}
				needSslEncrypted = _needSslEncrypted;
			}

			if (packets.size() >= targetBatchSize
				|| allBytebufferCapacity >= MAX_CAPACITY_MAX
				|| sslChanged) {
				break;
			}
		}

		if (allBytebufferCapacity == 0) {
			return null;
		}

		// 返回 ByteBuffer 数组，子类决定是使用 gather write（TCP）还是合并（UDP）
		ByteBuffer[] bufferArray = byteBuffers.toArray(new ByteBuffer[0]);
		return new BatchEncodeResult(bufferArray, packets, needSslEncrypted != null && needSslEncrypted);
	}

	@Override
	public Queue<Packet> getMsgQueue() {
		return msgQueue;
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
