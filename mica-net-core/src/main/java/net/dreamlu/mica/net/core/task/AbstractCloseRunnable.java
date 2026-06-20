/*
 * Copyright 2020 t-io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.dreamlu.mica.net.core.task;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.intf.TioListener;
import net.dreamlu.mica.net.core.maintain.MaintainUtils;
import net.dreamlu.mica.net.utils.thread.pool.AbstractQueueRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * 关闭连接的抽象基类，模板方法模式。
 * 子类实现具体的取消任务、关闭连接、关闭后处理等步骤。
 *
 * @author tanyaowu
 * @see TcpCloseRunnable TCP 实现（关闭 socket、重连等）
 * @see UdpCloseRunnable UDP 实现（仅清理，无连接概念）
 */
public abstract class AbstractCloseRunnable extends AbstractQueueRunnable<ChannelContext> {
	private static final Logger log = LoggerFactory.getLogger(AbstractCloseRunnable.class);

	/**
	 * The msg queue.
	 */
	private final Queue<ChannelContext> msgQueue;

	protected AbstractCloseRunnable(Executor executor) {
		super(executor);
		this.msgQueue = new ConcurrentLinkedQueue<>();
	}

	@Override
	public void runTask() {
		if (msgQueue.isEmpty()) {
			return;
		}
		ChannelContext channelContext;
		while ((channelContext = msgQueue.poll()) != null) {
			try {
				boolean isNeedRemove = channelContext.closeMeta.isNeedRemove;
				String remark = channelContext.closeMeta.remark;
				Throwable throwable = channelContext.closeMeta.throwable;

				channelContext.stat.timeClosed = System.currentTimeMillis();

				// 监听器
				TioListener tioListener = channelContext.tioConfig.getTioListener();
				if (tioListener != null) {
					try {
						tioListener.onBeforeClose(channelContext, throwable, remark, isNeedRemove);
					} catch (Throwable e) {
						log.error(e.getMessage(), e);
					}
				}
				try {
					if (channelContext.isClosed() && !isNeedRemove) {
						log.info("{}, {}已经关闭，备注:{}，异常:{}", channelContext.tioConfig, channelContext, remark, throwable == null ? "无" : throwable.toString());
						return;
					}
					if (channelContext.isRemoved()) {
						log.info("{}, {}已经删除，备注:{}，异常:{}", channelContext.tioConfig, channelContext, remark, throwable == null ? "无" : throwable.toString());
						return;
					}

					// 1. 取消任务（子类实现）
					doCancelTasks(channelContext);

					// 2. 清空队列（通用逻辑，TCP/UDP 相同）
					channelContext.getDecodeRunnable().clearMsgQueue();
					channelContext.getHandlerRunnable().clearMsgQueue();
					channelContext.getSendRunnable().clearMsgQueue();

					log.info("{}, {} 准备关闭连接, isNeedRemove:{}, {}", channelContext.tioConfig, channelContext, isNeedRemove, remark);

					// 3. 关闭连接（子类实现）
					doCloseConnection(channelContext, isNeedRemove);

					channelContext.setRemoved(isNeedRemove);
					if (channelContext.tioConfig.statOn) {
						channelContext.tioConfig.groupStat.closed.increment();
					}
					channelContext.stat.timeClosed = System.currentTimeMillis();
					channelContext.setClosed(true);
				} catch (Throwable e) {
					log.error(throwable == null ? remark : throwable.getMessage(), e);
				} finally {
					// 4. 关闭后处理（子类实现，如 TCP 重连）
					doAfterClose(channelContext, isNeedRemove);
				}
			} finally {
				channelContext.setWaitingClose(false);
			}
		}
	}

	/**
	 * 取消任务（decode / handler / send）
	 *
	 * @param channelContext ChannelContext
	 */
	protected abstract void doCancelTasks(ChannelContext channelContext);

	/**
	 * 关闭连接，维护 connections / closeds 等集合
	 *
	 * @param channelContext ChannelContext
	 * @param isNeedRemove 是否需要移除
	 */
	protected abstract void doCloseConnection(ChannelContext channelContext, boolean isNeedRemove);

	/**
	 * 关闭后的处理（finally 块）
	 * <p>
	 * TCP 实现：条件满足时加入重连队列
	 * UDP 实现：无操作
	 *
	 * @param channelContext ChannelContext
	 * @param isNeedRemove 是否需要移除
	 */
	protected abstract void doAfterClose(ChannelContext channelContext, boolean isNeedRemove);

	@Override
	public String logstr() {
		return super.logstr();
	}

	@Override
	public Queue<ChannelContext> getMsgQueue() {
		return msgQueue;
	}
}
