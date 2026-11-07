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

package net.dreamlu.mica.net.http.test;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.Tio;
import net.dreamlu.mica.net.http.common.HttpRequest;
import net.dreamlu.mica.net.http.common.HttpResponse;
import net.dreamlu.mica.net.http.common.HttpResponseStatus;
import net.dreamlu.mica.net.utils.thread.ThreadUtils;
import net.dreamlu.mica.net.websocket.common.Ws;
import net.dreamlu.mica.net.websocket.common.WsCloseCode;
import net.dreamlu.mica.net.websocket.common.WsRequest;
import net.dreamlu.mica.net.websocket.common.WsResponse;
import net.dreamlu.mica.net.websocket.server.WsServerStarter;
import net.dreamlu.mica.net.websocket.server.handler.IWsMsgHandler;

import java.nio.charset.StandardCharsets;

/**
 * WebSocket 测试服务，可直接 main 启动。
 * <p>
 * 演示 RFC 6455 协议层的各项能力：文本/二进制收发、ping/pong、主动推送、
 * 关闭握手、子协议协商、握手阶段拦截。
 *
 * @author L.cm
 */
public class WsExample {

	public static void main(String[] args) throws Exception {
		int port = args.length > 0 ? Integer.parseInt(args[0]) : 8182;
		WsServerStarter starter = new WsServerStarter(port, new WsExampleHandler());
		starter.start();
		System.out.println("WebSocket Server started on ws://localhost:" + port);
		System.out.println("Try (in browser console):");
		System.out.println("  let ws = new WebSocket('ws://localhost:" + port + "');");
		System.out.println("  ws.onmessage = e => console.log('recv', e.data);");
		System.out.println("  ws.onopen = () => ws.send('hello');");
		System.out.println("支持的测试指令：");
		System.out.println("  hello          普通文本，回显 hello:hello");
		System.out.println("  ping          服务端发 ping 帧，收到 pong 会打印到控制台");
		System.out.println("  push          服务端连续推送 10 条消息");
		System.out.println("  close         发起关闭握手，服务端与客户端互相回 Close 帧");
	}

	/**
	 * 业务处理器
	 */
	static class WsExampleHandler implements IWsMsgHandler {

		/**
		 * 声明支持的子协议，客户端请求时会协商选中
		 */
		@Override
		public String[] getSupportedSubProtocols() {
			return new String[]{"chat", "superchat"};
		}

		/**
		 * 握手阶段可以校验 token，不通过则用非 101 的响应拒绝升级
		 */
		@Override
		public HttpResponse handshake(HttpRequest request, HttpResponse response, ChannelContext context) {
			String token = request.getParam("token");
			if (token != null && !"secret".equals(token)) {
				response.setStatus(HttpResponseStatus.C401);
				return response;
			}
			return response;
		}

		@Override
		public void onAfterHandshaked(HttpRequest request, HttpResponse response, ChannelContext context) {
			System.out.println("客户端已连接：" + context.getClientNode());
		}

		@Override
		public Object onText(WsRequest request, String text, ChannelContext context) {
			switch (text) {
				case "ping":
					// 主动发 ping 帧，框架收到对端的 pong 会回调 onPong
					Ws.ping(context, "are you there?".getBytes(StandardCharsets.UTF_8));
					return "ping sent";
				case "push":
					// 服务端主动推送
					ThreadUtils.sleep(200);
					new Thread(() -> {
						for (int i = 0; i < 10; i++) {
							Tio.send(context, WsResponse.fromText("tick-" + i, StandardCharsets.UTF_8));
							ThreadUtils.sleep(1000);
						}
					}).start();
					return "pushing";
				case "close":
					// 优雅关闭：先发 Close 帧，等对端回应后再断开 TCP 连接
					Ws.close(context, WsCloseCode.NORMAL, "服务端主动关闭");
					return null;
				default:
					return "echo:" + text;
			}
		}

		@Override
		public Object onBytes(WsRequest request, byte[] bytes, ChannelContext context) {
			// 二进制消息原样回显
			return bytes;
		}

		/**
		 * 收到 pong 帧（框架对 ping 的自动应答不会回调这里，这里是对端主动发来的）
		 */
		@Override
		public void onPong(WsRequest request, byte[] bytes, ChannelContext context) {
			System.out.println("收到 pong：" + new String(bytes, StandardCharsets.UTF_8));
		}

		@Override
		public Object onClose(WsRequest request, byte[] bytes, ChannelContext context) {
			System.out.println("客户端关闭了连接，关闭码：" + request.getWsCloseCode());
			return null;
		}
	}
}
