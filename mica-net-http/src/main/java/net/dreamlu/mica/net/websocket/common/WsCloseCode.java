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

/**
 * WebSocket 关闭码，参考 RFC 6455 §7.4.1 <a href="https://tools.ietf.org/html/rfc6455#section-7.4.1">Status Codes</a>。
 *
 * <p>其中 1004、1005、1006、1015 是 RFC 保留码，端点不得将其作为 Close 帧的状态码发出；
 * 1016~2999 属于协议/扩展保留区间，同样不允许在 Close 帧中携带。</p>
 *
 * @author L.cm
 */
public final class WsCloseCode {

	/**
	 * 1000，正常关闭，表示连接已经完成了它应该做的任务
	 */
	public static final int NORMAL = 1000;
	/**
	 * 1001，端点正在离开（如服务端下线、浏览器关闭页面）
	 */
	public static final int GOING_AWAY = 1001;
	/**
	 * 1002，协议错误，收到不符合协议规范的帧
	 */
	public static final int PROTOCOL_ERROR = 1002;
	/**
	 * 1003，数据类型不支持，收到无法接受的数据类型（如只支持文本却收到二进制）
	 */
	public static final int UNSUPPORTED_DATA = 1003;
	/**
	 * 1004，保留码，不得使用
	 */
	public static final int RESERVED = 1004;
	/**
	 * 1005，没有收到状态码，仅用于本地表示「对端未提供状态码」，不得作为 Close 帧的状态码发出
	 */
	public static final int NO_STATUS = 1005;
	/**
	 * 1006，连接异常关闭，仅用于本地表示「TCP 连接异常断开」，不得作为 Close 帧的状态码发出
	 */
	public static final int ABNORMAL = 1006;
	/**
	 * 1007，无效的负载数据，收到无法解析成约定格式的数据（如非 UTF-8 的文本帧）
	 */
	public static final int INVALID_PAYLOAD = 1007;
	/**
	 * 1008，策略违规，收到违反端点策略的消息
	 */
	public static final int POLICY_VIOLATION = 1008;
	/**
	 * 1009，消息太大，消息体超过端点允许的大小
	 */
	public static final int MESSAGE_TOO_BIG = 1009;
	/**
	 * 1010，缺少必需的扩展，客户端要求的扩展服务端不支持
	 */
	public static final int MANDATORY_EXTENSION = 1010;
	/**
	 * 1011，服务端内部错误，服务端在处理消息时出现意外情况
	 */
	public static final int INTERNAL_ERROR = 1011;
	/**
	 * 1012，服务重启，服务端正在重启
	 */
	public static final int SERVICE_RESTART = 1012;
	/**
	 * 1013，请稍后重试，服务端过载或正在维护
	 */
	public static final int TRY_AGAIN_LATER = 1013;
	/**
	 * 1014，网关错误，服务端作为网关时上游返回了无效响应
	 */
	public static final int BAD_GATEWAY = 1014;
	/**
	 * 1015，TLS 握手失败，仅用于本地表示「TLS 握手失败」，不得作为 Close 帧的状态码发出
	 */
	public static final int TLS_HANDSHAKE = 1015;

	private WsCloseCode() {
	}

	/**
	 * 判断关闭码是否**不**允许出现在 Close 帧中。
	 *
	 * <p>不允许的范围：1000 以下的区间、1004 / 1005 / 1006 / 1015 等保留码，
	 * 以及 1016~2999 的协议保留区间。</p>
	 *
	 * @param closeCode 关闭码
	 * @return true 表示不能作为 Close 帧的状态码
	 */
	public static boolean isInvalid(int closeCode) {
		if (closeCode >= 1000 && closeCode <= 1003) {
			return false;
		}
		if (closeCode >= 1007 && closeCode <= 1014) {
			return false;
		}
		// 3000~3999 供库/框架/应用使用，4000~4999 供私有使用
		return closeCode < 3000 || closeCode > 4999;
	}

	/**
	 * 判断关闭码是否允许出现在 Close 帧中。
	 *
	 * <p>允许的范围：1000~1003、1007~1014，以及 3000~4999（库/框架/应用私有区间）。</p>
	 *
	 * @param closeCode 关闭码
	 * @return true 表示可以作为 Close 帧的状态码
	 */
	public static boolean isValid(int closeCode) {
		return !isInvalid(closeCode);
	}
}
