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
package net.dreamlu.mica.net.core.udp;

import net.dreamlu.mica.net.core.task.AbstractCloseRunnable;
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.maintain.MaintainUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

/**
 * UDP 关闭连接的简化实现。
 * <p>
 * UDP 是无连接协议，无需关闭 socket、无需重连。
 * 仅需取消任务、清空队列、从连接集合中移除即可。
 *
 * @author tanyaowu
 */
public class UdpCloseRunnable extends AbstractCloseRunnable {
	private static final Logger log = LoggerFactory.getLogger(UdpCloseRunnable.class);

	public UdpCloseRunnable(Executor executor) {
		super(executor);
	}

	@Override
	protected void doCancelTasks(ChannelContext channelContext) {
		// UDP 无需 resetWriting（UdpSendRunnable 没有 writing 状态）
		channelContext.getDecodeRunnable().setCanceled(true);
		channelContext.getHandlerRunnable().setCanceled(true);
		channelContext.getSendRunnable().setCanceled(true);
	}

	@Override
	protected void doCloseConnection(ChannelContext channelContext, boolean isNeedRemove) {
		try {
			// UDP 无连接概念，无论 isNeedRemove 为何值，均直接移除
			MaintainUtils.remove(channelContext);
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	protected void doAfterClose(ChannelContext channelContext, boolean isNeedRemove) {
		// UDP 无重连概念，无需任何操作
	}
}
