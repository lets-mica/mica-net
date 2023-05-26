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

package org.tio.server.proxy;

import org.tio.core.ChannelContext;
import org.tio.core.exception.TioDecodeException;
import org.tio.utils.buffer.ByteBufferUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 开启 nginx 代理协议时需要开启，转发代理 ip 信息
 *
 * <p>
 * proxy_protocol on;
 * </p>
 *
 * @author L.cm
 */
public final class ProxyProtocolDecoder {
	/**
	 * Maximum possible length of a v1 proxy protocol header per spec
	 */
	private static final int V1_MAX_LENGTH = 108;
	/**
	 * 开启 proxy_protocol 的 key
	 */
	private static final String PROXY_PROTOCOL_KEY = "proxy_protocol_key";

	/**
	 * 开始 proxy protocol
	 *
	 * @param context ChannelContext
	 */
	public static void enableProxyProtocol(ChannelContext context) {
		context.set(PROXY_PROTOCOL_KEY, (byte) 1);
	}

	/**
	 * 判断是否开启 proxy protocol
	 *
	 * @param context ChannelContext
	 * @return 是否开启
	 */
	public static boolean isProxyProtocolEnabled(ChannelContext context) {
		return context.containsKey(PROXY_PROTOCOL_KEY);
	}

	/**
	 * 解码 proxy protocol
	 *
	 * @param buffer         ByteBuffer
	 * @param position       position
	 * @param readableLength readableLength
	 * @param context        ChannelContext
	 * @return ProxyProtocolMessage
	 * @throws TioDecodeException TioDecodeException
	 */
	public ProxyProtocolMessage decode(ByteBuffer buffer, int position, int readableLength, ChannelContext context) throws TioDecodeException {
		int endOfLine = findEndOfLine(buffer);
		// 判断超长的情况，有可能是半包，多次进入
		if (readableLength > V1_MAX_LENGTH) {
			throw new TioDecodeException("Error v1 proxy protocol, readableLength: " + readableLength);
		}
		// 有可能半包，所以返回 null
		if (endOfLine == -1) {
			return null;
		}
		buffer.mark();
		buffer.position(position);
		// PROXY TCP4 192.168.0.1 192.168.0.11 56324 443\r\n
		String header = ByteBufferUtil.readString(buffer, endOfLine, StandardCharsets.US_ASCII);
		// 清除，单元测试时为 null
		if (context != null) {
			context.remove(PROXY_PROTOCOL_KEY);
		}
		buffer.reset();
		String[] parts = header.split(" ");
		int numParts = parts.length;
		if (numParts < 2) {
			throw new TioDecodeException("invalid header: " + header + " (expected: 'PROXY' and proxied protocol values)");
		}
		if (!"PROXY".equals(parts[0])) {
			throw new TioDecodeException("unknown identifier: " + parts[0]);
		}
		String protoAndFam = parts[1];
		if (!"TCP4".equals(protoAndFam) && !"TCP6".equals(protoAndFam) && !"UNKNOWN".equals(protoAndFam)) {
			throw new TioDecodeException("unsupported v1 proxied protocol: " + protoAndFam);
		}
		if ("UNKNOWN".equals(protoAndFam)) {
			return unknownMsg();
		}
		if (numParts != 6) {
			throw new TioDecodeException("invalid TCP4/6 header: " + header + " (expected: 6 parts)");
		}
		return new ProxyProtocolMessage(protoAndFam, parts[2], parts[3], parts[4], parts[5]);
	}

	/**
	 * Proxy protocol message for 'UNKNOWN' proxied protocols. Per spec, when the proxied protocol is
	 * 'UNKNOWN' we must discard all other header values.
	 */
	private static ProxyProtocolMessage unknownMsg() {
		return new ProxyProtocolMessage("UNKNOWN", null, null, 0, 0);
	}

	/**
	 * Returns the index in the buffer of the end of line found.
	 * Returns -1 if no end of line was found in the buffer.
	 */
	private static int findEndOfLine(final ByteBuffer buffer) {
		final int n = buffer.limit();
		for (int i = buffer.position(); i < n; i++) {
			final byte b = buffer.get(i);
			if (b == '\r' && i < n - 1 && buffer.get(i + 1) == '\n') {
				return i;  // \r\n
			}
		}
		return -1;  // Not found.
	}

}
