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

package org.tio.server.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.core.stat.GroupStat;
import org.tio.core.task.HeartbeatMode;
import org.tio.server.ServerGroupStat;
import org.tio.server.TioServerConfig;
import org.tio.server.intf.TioServerListener;
import org.tio.utils.SysConst;
import org.tio.utils.timer.Timer;
import org.tio.utils.timer.TimerTask;

import java.util.Set;

/**
 * 服务端心跳，采用时间轮重构
 *
 * @author L.cm
 */
public class ServerHeartbeatTask extends TimerTask {
	private static final Logger log = LoggerFactory.getLogger(ServerHeartbeatTask.class);
	private final Timer timer;
	private final TioServerConfig serverConfig;
	private final HeartbeatMode heartbeatMode;
	private final GroupStat groupStat;
	private final TioServerListener serverListener;

	public ServerHeartbeatTask(Timer timer, TioServerConfig serverConfig) {
		super(serverConfig.heartbeatTimeout);
		this.timer = timer;
		this.serverConfig = serverConfig;
		this.heartbeatMode = serverConfig.getHeartbeatMode();
		this.groupStat = serverConfig.getGroupStat();
		this.serverListener = serverConfig.getTioServerListener();
	}

	@Override
	public void run() {
		// 1. 已经停止，跳过
		if (serverConfig.isStopped()) {
			return;
		}
		// 2. 不需要心跳检测
		if (!serverConfig.isNeedCheckHeartbeat()) {
			return;
		}
		// 3. 添加 task，保持后续执行
		timer.add(this);
		// 心跳检测
		long start = System.currentTimeMillis();
		Set<ChannelContext> contextSet = serverConfig.connections;
		long heartbeatTimeout = serverConfig.heartbeatTimeout;
		long start1 = 0;
		int count = 0;
		long decodeQueueSizeAll = 0;
		long handlerQueueSizeAll = 0;
		long sendQueueSizeAll = 0;
		try {
			start1 = System.currentTimeMillis();
			for (ChannelContext channelContext : contextSet) {
				count++;
				long compareTime = heartbeatMode.getLastTime(channelContext.stat);
				long currTime = System.currentTimeMillis();
				long interval = currTime - compareTime;
				boolean needRemove;
				if (channelContext.heartbeatTimeout != null && channelContext.heartbeatTimeout > 0) {
					needRemove = interval > channelContext.heartbeatTimeout;
				} else {
					needRemove = interval > heartbeatTimeout;
				}
				if (needRemove && !serverListener.onHeartbeatTimeout(channelContext, interval, channelContext.stat.heartbeatTimeoutCount.incrementAndGet())) {
					log.info("{}, {} ms没有收发消息", channelContext, interval);
					channelContext.setCloseCode(ChannelContext.CloseCode.HEARTBEAT_TIMEOUT);
					Tio.remove(channelContext, interval + " ms没有收发消息");
				} else {
					// 服务端队列数据统计
					int decodeQueueSize = channelContext.getDecodeQueueSize();
					if (decodeQueueSize > 0) {
						decodeQueueSizeAll += decodeQueueSize;
					}
					int handlerQueueSize = channelContext.getHandlerQueueSize();
					if (handlerQueueSize > 0) {
						handlerQueueSizeAll += handlerQueueSize;
					}
					int sendQueueSize = channelContext.getSendQueueSize();
					if (sendQueueSize > 0) {
						sendQueueSizeAll += sendQueueSize;
					}
				}
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		} finally {
			try {
				if (serverConfig.debug && log.isWarnEnabled()) {
					StringBuilder builder = new StringBuilder();
					builder.append(SysConst.CRLF).append(serverConfig.getName());
					builder.append("\r\n ├ 当前时间 :").append(System.currentTimeMillis());
					builder.append("\r\n ├ 连接统计");
					builder.append("\r\n │ \t ├ 共接受过连接数 :").append(((ServerGroupStat) groupStat).accepted.sum());
					builder.append("\r\n │ \t ├ 当前连接数 :").append(contextSet.size());
					builder.append("\r\n │ \t └ 关闭过的连接数 :").append(groupStat.closed.sum());
					builder.append("\r\n ├ 消息统计");
					builder.append("\r\n │ \t ├ 已处理消息 :").append(groupStat.handledPackets.sum());
					builder.append("\r\n │ \t ├ 已接收消息(packet/byte) :").append(groupStat.receivedPackets.sum()).append('/').append(groupStat.receivedBytes.sum());
					builder.append("\r\n │ \t ├ 已发送消息(packet/byte) :").append(groupStat.sentPackets.sum()).append('/').append(groupStat.sentBytes.sum()).append('b');
					builder.append("\r\n │ \t ├ 平均每次TCP包接收的字节数 :").append(groupStat.getBytesPerTcpReceive());
					builder.append("\r\n │ \t └ 平均每次TCP包接收的业务包 :").append(groupStat.getPacketsPerTcpReceive());
					builder.append("\r\n ├ 节点统计");
					builder.append("\r\n │ \t ├ clientNodes :").append(serverConfig.clientNodes.size());
					builder.append("\r\n │ \t ├ 所有连接 :").append(serverConfig.connections.size());
					builder.append("\r\n │ \t ├ 绑定user数 :").append(serverConfig.users.size());
					builder.append("\r\n │ \t ├ 绑定token数 :").append(serverConfig.tokens.size());
					builder.append("\r\n │ \t └ 等待同步消息响应 :").append(serverConfig.waitingResps.size());
					builder.append("\r\n ├ 队列统计");
					builder.append("\r\n │ \t ├ 解码队列总数 :").append(decodeQueueSizeAll);
					builder.append("\r\n │ \t ├ 处理队列总数 :").append(handlerQueueSizeAll);
					builder.append("\r\n │ \t └ 发送队列总数 :").append(sendQueueSizeAll);
					builder.append("\r\n └ 群组");
					builder.append("\r\n   \t └ groupmap: ").append(serverConfig.groups.size());
					log.warn(builder.toString());
					long end = System.currentTimeMillis();
					long iv1 = start1 - start;
					long iv = end - start1;
					log.warn("{}, 检查心跳, 共{}个连接, 取锁耗时{}ms, 循环耗时{}ms, 心跳超时时间:{}ms", serverConfig.getName(), count, iv1, iv, heartbeatTimeout);
				}
			} catch (Throwable e) {
				log.error(e.getMessage(), e);
			}
		}
	}

}
