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
import org.tio.core.Node;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.server.intf.DecoderFunction;
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
	 * PROXY UNKNOWN\r\n
	 */
	private static final String UNKNOWN = "UNKNOWN";

	private ProxyProtocolDecoder() {
	}

	/**
	 * 开始 proxy protocol
	 *
	 * @param context ChannelContext
	 */
	public static void enableProxyProtocol(ChannelContext context) {
		context.set(PROXY_PROTOCOL_KEY, (byte) 1);
	}

	/**
	 * 去掉 proxy protocol
	 *
	 * @param context ChannelContext
	 */
	public static void removeProxyProtocol(ChannelContext context) {
		context.remove(PROXY_PROTOCOL_KEY);
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
	 * 解码，如果开启了 proxy protocol
	 *
	 * @param context        ChannelContext
	 * @param buffer         ByteBuffer
	 * @param readableLength readableLength
	 * @param next           下一个解码器
	 * @return ProxyProtocolMessage
	 * @throws TioDecodeException TioDecodeException
	 */
	public static Packet decodeIfEnable(ChannelContext context, ByteBuffer buffer, int readableLength,
										DecoderFunction next) throws TioDecodeException {
		if (ProxyProtocolDecoder.isProxyProtocolEnabled(context)) {
			return decode(context, buffer, readableLength, next);
		} else {
			return next.apply(buffer, readableLength, context);
		}
	}

	/**
	 * 解码 proxy protocol
	 *
	 * @param context        ChannelContext
	 * @param buffer         ByteBuffer
	 * @param readableLength readableLength
	 * @param next           下一个解码器
	 * @return ProxyProtocolMessage
	 * @throws TioDecodeException TioDecodeException
	 */
	public static Packet decode(ChannelContext context, ByteBuffer buffer, int readableLength, DecoderFunction next) throws TioDecodeException {
		buffer.mark();
		ProxyProtocolMessage message;
		try {
			message = decodeMessage(buffer, readableLength);
		} catch (ProxyProtocolException e) {
			throw e;
		} catch (TioDecodeException e) {
			// 清除协议 key，重置 buffer
			context.remove(PROXY_PROTOCOL_KEY);
			buffer.reset();
			return next.apply(buffer, readableLength, context);
		}
		// 半包的情况
		if (message == null) {
			return null;
		}
		// 清除协议 key
		context.remove(PROXY_PROTOCOL_KEY);
		// 设置客户端代理节点
		String protocol = message.getProtocol();
		if (UNKNOWN.equals(protocol)) {
			context.setProxyClientNode(new Node(UNKNOWN, message.getDestinationPort()));
		} else {
			context.setClientNode(new Node(message.getSourceAddress(), message.getSourcePort()));
			context.setProxyClientNode(new Node(message.getDestinationAddress(), message.getDestinationPort()));
		}
		return next.apply(buffer, readableLength, context);
	}

	/**
	 * 解码 proxy protocol
	 *
	 * @param buffer         ByteBuffer
	 * @param readableLength readableLength
	 * @return ProxyProtocolMessage
	 * @throws TioDecodeException TioDecodeException
	 */
	public static ProxyProtocolMessage decodeMessage(ByteBuffer buffer, int readableLength) throws TioDecodeException {
		int endOfLine = findEndOfLine(buffer);
		// 判断超长的情况，有可能是半包，多次进入
		if (endOfLine > V1_MAX_LENGTH || (readableLength > V1_MAX_LENGTH && endOfLine == -1)) {
			throw new TioDecodeException("Error v1 proxy protocol, readableLength: " + readableLength);
		}
		// 有可能半包，所以返回 null
		if (endOfLine == -1) {
			return null;
		}
		// PROXY TCP4 192.168.0.1 192.168.0.11 56324 443\r\n
		String header = ByteBufferUtil.readString(buffer, endOfLine, StandardCharsets.US_ASCII);
		// 跳过 \r\n
		ByteBufferUtil.skipBytes(buffer, 2);
		String[] parts = header.split(" ");
		int numParts = parts.length;
		if (numParts < 2) {
			throw new TioDecodeException("invalid header: " + header + " (expected: 'PROXY' and proxied protocol values)");
		}
		if (!"PROXY".equals(parts[0])) {
			throw new TioDecodeException("unknown identifier: " + parts[0]);
		}
		String proxiedProtocol = parts[1];
		if (!"TCP4".equals(proxiedProtocol) && !"TCP6".equals(proxiedProtocol) && !UNKNOWN.equals(proxiedProtocol)) {
			throw new ProxyProtocolException("unsupported v1 proxied protocol: " + proxiedProtocol);
		}
		if (UNKNOWN.equals(proxiedProtocol)) {
			return unknownMsg();
		}
		if (numParts != 6) {
			throw new ProxyProtocolException("invalid TCP4/6 header: " + header + " (expected: 6 parts)");
		}
		return new ProxyProtocolMessage(proxiedProtocol, parts[2], parts[3], parts[4], parts[5]);
	}

	/**
	 * Proxy protocol message for 'UNKNOWN' proxied protocols. Per spec, when the proxied protocol is
	 * 'UNKNOWN' we must discard all other header values.
	 */
	private static ProxyProtocolMessage unknownMsg() {
		return new ProxyProtocolMessage(UNKNOWN, null, null, 0, 0);
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
