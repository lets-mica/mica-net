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

package net.dreamlu.mica.net.websocket.common;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.Tio;
import net.dreamlu.mica.net.core.TioConfig;
import net.dreamlu.mica.net.server.TioServerConfig;
import net.dreamlu.mica.net.utils.timer.TimerTask;
import net.dreamlu.mica.net.utils.timer.TimerTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * WebSocket 操作门面：提供服务端主动发送控制帧、优雅关闭连接的快捷方法。
 *
 * <p>RFC 6455 §5.5.1 规定：端点收到 Close 帧后必须回一个 Close 帧，
 * 并且在双方都完成关闭握手后关闭 TCP 连接。本类提供的 {@link #close} 即为
 * 服务端主动发起关闭握手（先发 Close 帧，再等待对端回应 Close 帧）。</p>
 *
 * @author L.cm
 */
public final class Ws {
	private static final Logger log = LoggerFactory.getLogger(Ws.class);

	/**
	 * 关闭握手等待对端回应的最长时间（毫秒），超时后强制断开 TCP 连接
	 */
	public static final long DEFAULT_CLOSE_TIMEOUT = 5000L;

	private Ws() {
	}

	/**
	 * 优雅关闭 WebSocket 连接，状态码为 1000（正常关闭）
	 *
	 * @param context 连接上下文
	 * @return true 表示已发起关闭握手
	 */
	public static boolean close(ChannelContext context) {
		return close(context, WsCloseCode.NORMAL, null);
	}

	/**
	 * 优雅关闭 WebSocket 连接：发送 Close 帧后等待对端回应 Close 帧，再关闭 TCP 连接
	 *
	 * @param context   连接上下文
	 * @param closeCode 关闭码
	 * @param reason    关闭原因，可为 null
	 * @return true 表示已发起关闭握手
	 */
	public static boolean close(ChannelContext context, int closeCode, String reason) {
		if (context == null) {
			return false;
		}
		WsSessionContext session = context.get(WsSessionContext.WS_SESSION_CONTEXT_KEY);
		if (session == null || !session.isHandshaked() || session.isCloseSent()) {
			return false;
		}
		if (!WsCloseCode.isValid(closeCode)) {
			// 1004 / 1005 / 1006 / 1015 等保留码不允许出现在 Close 帧中
			log.warn("{} 非法的 websocket 关闭码：{}，改用 {}", context, closeCode, WsCloseCode.NORMAL);
			closeCode = WsCloseCode.NORMAL;
		}
		WsResponse close = WsResponse.fromClose(closeCode, reason, StandardCharsets.UTF_8);
		session.setCloseSent(true);
		// Close 帧写出去之后开始计时，对端迟迟不应答则强制断开，避免连接泄漏
		close.setPacketListener((ctx, packet, isSentSuccess) -> scheduleCloseTimeout(ctx, session));
		return Tio.send(context, close);
	}

	/**
	 * 主动发送 Ping 帧
	 *
	 * @param context         连接上下文
	 * @param applicationData 附加数据，可为 null
	 * @return true 表示发送成功
	 */
	public static boolean ping(ChannelContext context, byte[] applicationData) {
		if (context == null) {
			return false;
		}
		return Tio.send(context, WsResponse.fromPing(applicationData));
	}

	/**
	 * 主动发送 Pong 帧
	 *
	 * @param context         连接上下文
	 * @param applicationData 附加数据，可为 null
	 * @return true 表示发送成功
	 */
	public static boolean pong(ChannelContext context, byte[] applicationData) {
		if (context == null) {
			return false;
		}
		return Tio.send(context, WsResponse.fromPong(applicationData));
	}

	/**
	 * 关闭握手超时处理：等待超时后强制关闭 TCP 连接
	 *
	 * @param context 连接上下文
	 * @param session 会话上下文
	 */
	static void scheduleCloseTimeout(ChannelContext context, WsSessionContext session) {
		TimerTaskService taskService = getTaskService(context);
		if (taskService == null) {
			// 没有可用的定时器，直接断开，保证连接不会一直挂着
			Tio.remove(context, "websocket 关闭握手完成");
			return;
		}
		taskService.addTask(systemTimer -> new TimerTask(DEFAULT_CLOSE_TIMEOUT) {
			@Override
			public void run() {
				if (!session.isCloseReceived() && !context.isClosed()) {
					Tio.remove(context, "websocket 关闭握手超时");
				}
			}
		});
	}

	/**
	 * 获取服务端定时任务服务
	 *
	 * @param context 连接上下文
	 * @return 定时任务服务，可能为 null
	 */
	private static TimerTaskService getTaskService(ChannelContext context) {
		TioConfig tioConfig = context.tioConfig;
		if (tioConfig instanceof TioServerConfig) {
			return ((TioServerConfig) tioConfig).getTaskService();
		}
		return null;
	}
}
