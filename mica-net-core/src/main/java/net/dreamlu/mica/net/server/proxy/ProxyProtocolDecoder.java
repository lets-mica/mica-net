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

package net.dreamlu.mica.net.server.proxy;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.Node;
import net.dreamlu.mica.net.core.exception.TioDecodeException;
import net.dreamlu.mica.net.core.intf.IgnorePacket;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.server.intf.DecoderFunction;
import net.dreamlu.mica.net.utils.buffer.ByteBufferUtil;

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
	 * 最小头 “PROXY ” 用来判定是否 v1 的协议
	 */
	private static final int V1_MIN_HEAD_LENGTH = 6;
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
		if (isProxyProtocolEnabled(context)) {
			return decode(context, buffer, readableLength, next);
		} else {
			return next.apply(context, buffer, readableLength);
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
		// 如果小于最小长度，尝试解析下一个
		if (readableLength < V1_MIN_HEAD_LENGTH) {
			return next.apply(context, buffer, readableLength);
		}
		// 标记
		buffer.mark();
		// PROXY TCP4 192.168.0.1 192.168.0.11 56324 443\r\n
		String proxyPrefix = ByteBufferUtil.readString(buffer, V1_MIN_HEAD_LENGTH, StandardCharsets.US_ASCII);
		// 非 PROXY 协议，直接返回
		if (!"PROXY ".equals(proxyPrefix)) {
			// 清除协议 key，重置 buffer
			context.remove(PROXY_PROTOCOL_KEY);
			buffer.reset();
			return next.apply(context, buffer, readableLength);
		}
		// 解析 proxy 协议
		ProxyProtocolMessage message = decodeMessage(buffer, readableLength);
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
		if (buffer.hasRemaining()) {
			return next.apply(context, buffer, readableLength);
		} else {
			return IgnorePacket.INSTANCE;
		}
	}

	/**
	 * 解码 proxy protocol
	 *
	 * @param buffer         ByteBuffer
	 * @param readableLength readableLength
	 * @return ProxyProtocolMessage
	 * @throws TioDecodeException TioDecodeException
	 */
	public static ProxyProtocolMessage decodeForTest(ByteBuffer buffer, int readableLength) throws TioDecodeException {
		// PROXY TCP4 192.168.0.1 192.168.0.11 56324 443\r\n
		String proxyPrefix = ByteBufferUtil.readString(buffer, V1_MIN_HEAD_LENGTH, StandardCharsets.US_ASCII);
		// 非 PROXY 协议，直接返回
		if (!"PROXY ".equals(proxyPrefix)) {
			throw new TioDecodeException("unknown identifier: " + proxyPrefix);
		}
		return decodeMessage(buffer, readableLength);
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
		// PROXY TCP4 192.168.0.1 192.168.0.11 56324 443\r\n 去除前缀 PROXY
		// TCP4 192.168.0.1 192.168.0.11 56324 443\r\n
		String header = ByteBufferUtil.readString(buffer, endOfLine - V1_MIN_HEAD_LENGTH, StandardCharsets.US_ASCII);
		// 跳过 \r\n
		ByteBufferUtil.skipBytes(buffer, 2);
		String[] parts = header.split(" ");
		int numParts = parts.length;
		if (numParts < 1) {
			throw new TioDecodeException("invalid header: PROXY " + header + " (expected: 'PROXY' and proxied protocol values)");
		}
		String proxyProtocol = parts[0];
		if (!"TCP4".equals(proxyProtocol) && !"TCP6".equals(proxyProtocol) && !UNKNOWN.equals(proxyProtocol)) {
			throw new TioDecodeException("unsupported v1 proxy protocol: " + proxyProtocol);
		}
		if (UNKNOWN.equals(proxyProtocol)) {
			return unknownMsg();
		}
		if (numParts != 5) {
			throw new TioDecodeException("invalid TCP4/6 header: PROXY " + header + " (expected: 6 parts)");
		}
		return new ProxyProtocolMessage(proxyProtocol, parts[1], parts[2], parts[3], parts[4]);
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
