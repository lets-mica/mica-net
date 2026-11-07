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

import net.dreamlu.mica.net.core.exception.TioDecodeException;

/**
 * WebSocket 协议错误异常。
 *
 * <p>按照 RFC 6455 §7.1.7「Fail the WebSocket Connection」，收到非法帧时，
 * 端点应先向对端发送携带对应状态码的 Close 帧，再关闭 TCP 连接。
 * 本异常携带该状态码，由上层捕获后完成「发 Close 帧 → 关闭连接」的动作。</p>
 *
 * @author L.cm
 */
public class WsProtocolException extends TioDecodeException {
	private static final long serialVersionUID = 6116995340170141667L;

	/**
	 * 应回复给对端的关闭码
	 */
	private final int closeCode;

	public WsProtocolException(int closeCode, String message) {
		super(message);
		this.closeCode = closeCode;
	}

	/**
	 * @param closeCode 应回复给对端的关闭码
	 * @param message   错误描述
	 * @param cause     原始异常
	 */
	public WsProtocolException(int closeCode, String message, Throwable cause) {
		super(message, cause);
		this.closeCode = closeCode;
	}

	/**
	 * @return 应回复给对端的关闭码
	 */
	public int getCloseCode() {
		return closeCode;
	}
}
