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
import net.dreamlu.mica.net.utils.timer.TimerTask;
import net.dreamlu.mica.net.utils.timer.TimerTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * WebSocket 操作门面：提供主动发送控制帧、优雅关闭连接的快捷方法。
 *
 * <p>RFC 6455 §5.5.1 规定：端点收到 Close 帧后必须回一个 Close 帧，
 * 并且在双方都完成关闭握手后关闭 TCP 连接。本类提供的 {@link #close} 即为
 * 主动发起关闭握手（先发 Close 帧，再等待对端回应 Close 帧）。等待对端回应依赖定时任务，
 * 取的是运行时实际使用的定时器（{@code TioConfig#getRuntimeTaskService()}），
 * 拿不到时退化为立即断开 TCP。</p>
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
		if (WsCloseCode.isInvalid(closeCode)) {
			// 1004 / 1005 / 1006 / 1015 等保留码不允许出现在 Close 帧中
			log.warn("{} 非法的 websocket 关闭码：{}，改用 {}", context, closeCode, WsCloseCode.NORMAL);
			closeCode = WsCloseCode.NORMAL;
		}
		WsResponse close = WsResponse.fromClose(closeCode, reason, StandardCharsets.UTF_8);
		session.setCloseSent(true);
		// Close 帧写出去之后开始计时，对端迟迟不应答则强制断开，避免连接泄漏
		close.setPacketListener((ctx, packet, isSentSuccess) -> scheduleCloseTimeout(ctx, session));
		boolean sent = Tio.send(context, close);
		if (!sent) {
			// Close 帧没能进发送队列，关闭握手无从谈起，直接断开，避免连接一直挂着
			Tio.remove(context, "websocket 关闭帧发送失败");
		}
		return sent;
	}

	/**
	 * 主动发送 Ping 帧
	 *
	 * @param context         连接上下文
	 * @param applicationData 附加数据，可为 null
	 * @return true 表示发送成功
	 */
	public static boolean ping(ChannelContext context, byte[] applicationData) {
		return sendControlFrame(context, Opcode.PING, applicationData);
	}

	/**
	 * 主动发送 Pong 帧
	 *
	 * @param context         连接上下文
	 * @param applicationData 附加数据，可为 null
	 * @return true 表示发送成功
	 */
	public static boolean pong(ChannelContext context, byte[] applicationData) {
		return sendControlFrame(context, Opcode.PONG, applicationData);
	}

	/**
	 * 发送控制帧（Ping / Pong）。
	 * <p>
	 * 负载长度在这里就校验：编码阶段虽然也会拒绝超长控制帧，但那个异常会被发送链路的
	 * {@code catch (Throwable)} 吞掉，调用方只能看到 {@code Tio.send} 返回 false 和一条 error 日志，
	 * 拿不到明确原因。前置校验让调用方直接拿到 false + warn 日志。
	 * </p>
	 *
	 * @param context         连接上下文
	 * @param opcode          控制帧操作码
	 * @param applicationData 附加数据，可为 null
	 * @return true 表示发送成功
	 */
	private static boolean sendControlFrame(ChannelContext context, Opcode opcode, byte[] applicationData) {
		if (context == null) {
			return false;
		}
		int length = applicationData == null ? 0 : applicationData.length;
		if (length > WsPacket.MAX_CONTROL_FRAME_BODY_LENGTH) {
			log.warn("{} websocket 控制帧负载不能超过 {} 字节，实际：{}，已丢弃该 {} 帧",
				context, WsPacket.MAX_CONTROL_FRAME_BODY_LENGTH, length, opcode);
			return false;
		}
		WsResponse response = opcode == Opcode.PING
			? WsResponse.fromPing(applicationData)
			: WsResponse.fromPong(applicationData);
		return Tio.send(context, response);
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
			Tio.remove(context, "websocket 关闭握手完成（无可用定时器）");
			return;
		}
		TimerTask task = taskService.addTask(systemTimer -> new TimerTask(DEFAULT_CLOSE_TIMEOUT) {
			@Override
			public void run() {
				if (!session.isCloseReceived() && !context.isClosed()) {
					Tio.remove(context, "websocket 关闭握手超时");
				}
			}
		});
		session.setCloseTimeoutTask(task);
		if (session.isCloseReceived()) {
			// 登记超时任务之前对端就可能已经回了 Close，补一次取消，别让任务白活 5 秒
			session.cancelCloseTimeout();
		}
	}

	/**
	 * 获取定时任务服务。
	 * <p>
	 * 取的是运行时实际使用的那一个（{@code TioConfig#getRuntimeTaskService()}）：
	 * 服务端启动后、客户端构造后才有值，用户显式设置的定时器优先，
	 * 否则是框架自建的。拿不到时退化为「立即断开 TCP」。
	 * </p>
	 *
	 * @param context 连接上下文
	 * @return 定时任务服务，可能为 null
	 */
	private static TimerTaskService getTaskService(ChannelContext context) {
		return context.tioConfig.getRuntimeTaskService();
	}
}
