/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & dreamlu.net).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tio.core.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.intf.IgnorePacket;
import org.tio.core.intf.Packet;
import org.tio.core.intf.TioHandler;
import org.tio.core.intf.TioListener;
import org.tio.utils.thread.pool.AbstractQueueRunnable;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * 解码任务抽象基类 - 提取 TCP/UDP 共享逻辑
 *
 * @author L.cm
 */
public abstract class AbstractDecodeRunnable extends AbstractQueueRunnable<ByteBuffer> {
	private static final Logger log = LoggerFactory.getLogger(AbstractDecodeRunnable.class);

	// 共享字段
	protected final ChannelContext channelContext;
	protected final TioConfig tioConfig;
	protected final TioHandler tioHandler;
	protected final TioListener tioListener;
	protected final Queue<ByteBuffer> msgQueue;

	/**
	 * 新收到的数据
	 */
	protected ByteBuffer newReceivedByteBuffer = null;

	public AbstractDecodeRunnable(ChannelContext channelContext, Executor executor) {
		super(executor);
		this.channelContext = channelContext;
		this.tioConfig = channelContext.tioConfig;
		this.tioHandler = this.tioConfig.getTioHandler();
		this.tioListener = this.tioConfig.getTioListener();
		this.msgQueue = tioConfig.useQueueDecode ? new ConcurrentLinkedQueue<>() : null;
	}

	/**
	 * 消息处理
	 *
	 * @param packet    Packet
	 * @param byteCount byteCount
	 */
	public void handler(Packet packet, int byteCount) {
		// 包处理方式，默认单线程
		switch (tioConfig.packetHandlerMode) {
			case QUEUE:
				channelContext.handlerRunnable.addMsg(packet);
				channelContext.handlerRunnable.execute();
				break;
			case SINGLE_THREAD:
			default:
				channelContext.handlerRunnable.handler(packet);
				break;
		}
	}

	/**
	 * 解码成功后的处理（公共逻辑）
	 */
	protected void onDecodeSuccess(Packet packet, int packetSize) {
		// 解码成功
		channelContext.setPacketNeededLength(null);
		channelContext.stat.latestTimeOfReceivedPacket = System.currentTimeMillis();
		channelContext.stat.decodeFailCount = 0;
		packet.setByteCount(packetSize);
		if (tioConfig.statOn) {
			tioConfig.groupStat.receivedPackets.increment();
			channelContext.stat.receivedPackets.incrementAndGet();
		}
		if (tioListener != null) {
			try {
				tioListener.onAfterDecoded(channelContext, packet, packetSize);
			} catch (Throwable e) {
				log.error(e.getMessage(), e);
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("{}, 解包获得一个packet:{}", channelContext, packet.logstr());
		}
		// 如果为非可忽略的包，执行处理方法
		if (!(packet instanceof IgnorePacket)) {
			handler(packet, packetSize);
		}
	}

	/**
	 * 设置新收到的 ByteBuffer
	 *
	 * @param newReceivedByteBuffer ByteBuffer
	 */
	public void setNewReceivedByteBuffer(ByteBuffer newReceivedByteBuffer) {
		this.newReceivedByteBuffer = newReceivedByteBuffer;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ':' + channelContext.toString();
	}

	@Override
	public String logstr() {
		return toString();
	}

	@Override
	public Queue<ByteBuffer> getMsgQueue() {
		return msgQueue;
	}

	/**
	 * 解码方法（由子类实现）
	 */
	public abstract void decode();
}
