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

package org.tio.client.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.client.ClientChannelContext;
import org.tio.client.ClientGroupStat;
import org.tio.client.TioClientConfig;
import org.tio.client.intf.TioClientHandler;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.core.intf.Packet;
import org.tio.core.task.HeartbeatMode;
import org.tio.utils.timer.Timer;
import org.tio.utils.timer.TimerTask;

import java.util.Set;

/**
 * 客户端心跳任务
 *
 * @author L.cm
 */
public class ClientHeartbeatTask extends TimerTask {
	private static final Logger logger = LoggerFactory.getLogger(ClientHeartbeatTask.class);
	private final Timer timer;
	private final TioClientConfig clientConfig;
	private final ClientGroupStat clientGroupStat;
	private final TioClientHandler tioHandler;
	private final String id;

	public ClientHeartbeatTask(Timer timer, TioClientConfig clientConfig) {
		super(clientConfig.heartbeatTimeout / 3);
		this.timer = timer;
		this.clientConfig = clientConfig;
		this.clientGroupStat = (ClientGroupStat) clientConfig.groupStat;
		this.tioHandler = clientConfig.getTioClientHandler();
		this.id = clientConfig.getId();
	}

	@Override
	public void run() {
		// 1. 已经停止，跳过
		if (clientConfig.isStopped()) {
			return;
		}
		// 2. 添加 task，保持后续执行
		timer.add(this);
		// 3. 心跳检测
		Set<ChannelContext> set = clientConfig.connecteds;
		HeartbeatMode heartbeatMode = clientConfig.getHeartbeatMode();
		// 4. 心跳超时策略
		HeartbeatTimeoutStrategy timeoutStrategy = clientConfig.getHeartbeatTimeoutStrategy();
		long currTime = System.currentTimeMillis();
		try {
			for (ChannelContext entry : set) {
				ClientChannelContext context = (ClientChannelContext) entry;
				if (context.isClosed() || context.isRemoved()) {
					continue;
				}
				long compareTime = heartbeatMode.getLastTime(context.stat);
				long interval = currTime - compareTime;
				// 超过一个心跳周期，HeartbeatMode.LAST_RESP 在拔网线会出现这种情况，https://gitee.com/dromara/mica-mqtt/issues/IBSMZ7
				if (interval > clientConfig.heartbeatTimeout) {
					if (HeartbeatTimeoutStrategy.CLOSE == timeoutStrategy) {
						// 心跳超时策略为关闭连接
						context.setCloseCode(ChannelContext.CloseCode.HEARTBEAT_TIMEOUT);
						Tio.close(context, interval + "ms没有收到消息");
					} else {
						// 发送心跳
						sendHeartbeat(context);
					}
				} else if (interval >= clientConfig.heartbeatTimeout / 2) {
					// 超过半个心跳周期，发送心跳
					sendHeartbeat(context);
				}
			}
			// 打印连接信息
			if (clientConfig.debug && logger.isInfoEnabled()) {
				if (clientConfig.statOn) {
					logger.info("[{}]: curr:{}, closed:{}, received:({}p)({}b), handled:{}, sent:({}p)({}b)", id, set.size(), clientGroupStat.closed.sum(),
						clientGroupStat.receivedPackets.sum(), clientGroupStat.receivedBytes.sum(), clientGroupStat.handledPackets.sum(),
						clientGroupStat.sentPackets.sum(), clientGroupStat.sentBytes.sum());
				} else {
					logger.info("[{}]: curr:{}, closed:{}", id, set.size(), clientGroupStat.closed.sum());
				}
			}
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * 发送心跳包
	 *
	 * @param context ChannelContext
	 */
	private void sendHeartbeat(ChannelContext context) {
		Packet packet = tioHandler.heartbeatPacket(context);
		if (packet != null) {
			boolean result = Tio.send(context, packet);
			if (clientConfig.debug && logger.isInfoEnabled()) {
				logger.info("{} 发送心跳包 result:{}", context, result);
			}
		}
	}
}
