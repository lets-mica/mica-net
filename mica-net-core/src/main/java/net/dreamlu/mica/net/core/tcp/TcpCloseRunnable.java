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
package net.dreamlu.mica.net.core.tcp;

import net.dreamlu.mica.net.core.task.AbstractCloseRunnable;
import net.dreamlu.mica.net.client.ClientChannelContext;
import net.dreamlu.mica.net.client.ReconnConf;
import net.dreamlu.mica.net.client.TioClientConfig;
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.maintain.MaintainUtils;
import net.dreamlu.mica.net.core.task.AbstractSendRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

/**
 * TCP 关闭连接的具体实现。
 * <p>
 * 负责关闭 socket、维护连接集合、以及触发重连。
 *
 * @author tanyaowu
 */
public class TcpCloseRunnable extends AbstractCloseRunnable {
	private static final Logger log = LoggerFactory.getLogger(TcpCloseRunnable.class);

	public TcpCloseRunnable(Executor executor) {
		super(executor);
	}

	@Override
	protected void doCancelTasks(ChannelContext channelContext) {
		channelContext.getDecodeRunnable().setCanceled(true);
		channelContext.getHandlerRunnable().setCanceled(true);
		// 关闭前重置 writing 状态，防止 write 挂起导致 writing 未复位，
		// 重连后 runTask() 因 writing==true 直接 return
		// （ConnectionCompletionHandler 重连分支也会调 resetWriting()，此处为防御性双重保护）
		AbstractSendRunnable sendRunnable = channelContext.getSendRunnable();
		if (sendRunnable instanceof TcpSendRunnable) {
			((TcpSendRunnable) sendRunnable).resetWriting();
		}
		sendRunnable.setCanceled(true);
	}

	@Override
	protected void doCloseConnection(ChannelContext channelContext, boolean isNeedRemove) {
		try {
			if (isNeedRemove) {
				MaintainUtils.remove(channelContext);
			} else {
				TioClientConfig tioClientConfig = (TioClientConfig) channelContext.tioConfig;
				tioClientConfig.closeds.add(channelContext);
				tioClientConfig.connecteds.remove(channelContext);
				MaintainUtils.close(channelContext);
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	protected void doAfterClose(ChannelContext channelContext, boolean isNeedRemove) {
		// 不删除且没有连接上，则加到重连队列中
		if (!isNeedRemove && channelContext.isClosed() && !channelContext.isServer()) {
			ClientChannelContext clientChannelContext = (ClientChannelContext) channelContext;
			ReconnConf.put(clientChannelContext);
		}
	}
}
