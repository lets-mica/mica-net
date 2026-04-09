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
import java.util.Arrays;

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
	 * 最小头 “PROXY “ 用来判定是否 v1 的协议
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

	// ==================== V2 Protocol Constants ====================
	/**
	 * V2 固定签名: \x0D \x0A \x0D \x0A \x00 \x0D \x0A \x51 \x55 \x49 \x54 \x0A
	 */
	private static final byte[] V2_SIGNATURE = new byte[] {
		0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A
	};
	/**
	 * V2 固定头部长度
	 */
	private static final int V2_HEADER_LENGTH = 16;
	/**
	 * V2 最小头部检测长度
	 */
	private static final int V2_MIN_HEAD_LENGTH = 16;

	// V2 命令 (低4位)
	private static final byte V2_CMD_LOCAL = 0x00;
	private static final byte V2_CMD_PROXY = 0x01;

	// V2 地址族 (高4位)
	private static final byte V2_AF_UNSPEC = 0x00;
	private static final byte V2_AF_INET = 0x10;   // IPv4
	private static final byte V2_AF_INET6 = 0x20;  // IPv6
	private static final byte V2_AF_UNIX = 0x30;   // UNIX

	// V2 协议 (低4位)
	private static final byte V2_PROTO_UNSPEC = 0x00;
	private static final byte V2_PROTO_STREAM = 0x01;  // TCP/SOCK_STREAM
	private static final byte V2_PROTO_DGRAM = 0x02;    // UDP/SOCK_DGRAM

	// V2 地址长度
	private static final int V2_ADDR_LEN_IPV4 = 12;  // 4+4+2+2
	private static final int V2_ADDR_LEN_IPV6 = 36;  // 16+16+2+2
	private static final int V2_ADDR_LEN_UNIX = 216; // 108+108

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

		// 优先检测 V2 签名
		if (readableLength >= V2_MIN_HEAD_LENGTH && isV2Signature(buffer)) {
			return decodeV2(context, buffer, readableLength, next);
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

	// ==================== V2 Protocol Implementation ====================

	/**
	 * 检测 V2 签名
	 */
	private static boolean isV2Signature(ByteBuffer buffer) {
		buffer.mark();
		byte[] sig = ByteBufferUtil.readBytes(buffer, 12);
		buffer.reset();
		return Arrays.equals(sig, V2_SIGNATURE);
	}

	/**
	 * 解码 V2 proxy protocol
	 *
	 * @param context        ChannelContext
	 * @param buffer         ByteBuffer
	 * @param readableLength readableLength
	 * @param next           下一个解码器
	 * @return Packet
	 * @throws TioDecodeException TioDecodeException
	 */
	private static Packet decodeV2(ChannelContext context, ByteBuffer buffer, int readableLength, DecoderFunction next) throws TioDecodeException {
		// 检查最小长度
		if (readableLength < V2_HEADER_LENGTH) {
			return next.apply(context, buffer, readableLength);
		}

		// 标记
		buffer.mark();
		// 读取 V2 头部，跳过 12 字节签名
		ByteBufferUtil.skipBytes(buffer, 12);

		// 读取版本和命令
		byte verCmd = buffer.get();
		byte version = (byte) ((verCmd & 0xF0) >> 4);
		byte cmd = (byte) (verCmd & 0x0F);

		// 版本必须是 2
		if (version != 2) {
			throw new TioDecodeException("invalid v2 proxy protocol version: " + version);
		}

		// 读取地址族和协议
		byte fam = buffer.get();

		// 读取地址长度 (网络字节序 = 大端)
		short addrLen = ByteBufferUtil.readShortBE(buffer);
		// 检查数据完整性: 16(header) + addrLen
		int totalLength = V2_HEADER_LENGTH + addrLen;
		if (readableLength < totalLength) {
			buffer.reset();
			// 包长度不够，不够读
			return null;
		}
		// 根据命令处理
		if (cmd == V2_CMD_LOCAL) {
			// LOCAL: 跳过地址信息
			ByteBufferUtil.skipBytes(buffer, addrLen);
			// 清除协议 key
			context.remove(PROXY_PROTOCOL_KEY);
		} else if (cmd == V2_CMD_PROXY) {
			// PROXY: 解析地址
			decodeV2Address(context, buffer, fam, addrLen);
			// 清除协议 key
			context.remove(PROXY_PROTOCOL_KEY);
		} else {
			throw new TioDecodeException("invalid v2 proxy protocol command: " + cmd);
		}

		// 跳过 TLV (如果有)
		int tlvsLength = readableLength - totalLength;
		if (tlvsLength > 0) {
			ByteBufferUtil.skipBytes(buffer, tlvsLength);
		}

		if (buffer.hasRemaining()) {
			return next.apply(context, buffer, readableLength);
		} else {
			return IgnorePacket.INSTANCE;
		}
	}

	/**
	 * 解码 V2 地址
	 */
	private static void decodeV2Address(ChannelContext context, ByteBuffer buffer, byte fam, int addrLen) throws TioDecodeException {
		int addrFamily = fam & 0xFF & 0xF0;
		int proto = fam & 0x0F;

		if (addrFamily == V2_AF_INET) {
			if (proto == V2_PROTO_STREAM || proto == V2_PROTO_DGRAM) {
				// IPv4: 4+4+2+2 = 12 bytes
				if (addrLen < V2_ADDR_LEN_IPV4) {
					throw new TioDecodeException("invalid v2 ipv4 address length: " + addrLen);
				}
				byte[] srcAddr = new byte[4];
				byte[] dstAddr = new byte[4];
				buffer.get(srcAddr);
				buffer.get(dstAddr);
				int srcPort = ByteBufferUtil.readUnsignedShortBE(buffer);
				int dstPort = ByteBufferUtil.readUnsignedShortBE(buffer);

				String srcIp = bytesToIp(srcAddr);
				String dstIp = bytesToIp(dstAddr);

				context.setClientNode(new Node(srcIp, srcPort));
				context.setProxyClientNode(new Node(dstIp, dstPort));
			} else {
				throw new TioDecodeException("unsupported v2 protocol for IPv4: " + proto);
			}
		} else if (addrFamily == V2_AF_INET6) {
			if (proto == V2_PROTO_STREAM || proto == V2_PROTO_DGRAM) {
				// IPv6: 16+16+2+2 = 36 bytes
				if (addrLen < V2_ADDR_LEN_IPV6) {
					throw new TioDecodeException("invalid v2 ipv6 address length: " + addrLen);
				}
				byte[] srcAddr = new byte[16];
				byte[] dstAddr = new byte[16];
				buffer.get(srcAddr);
				buffer.get(dstAddr);
				int srcPort = ByteBufferUtil.readUnsignedShortBE(buffer);
				int dstPort = ByteBufferUtil.readUnsignedShortBE(buffer);

				String srcIp = bytesToIpv6(srcAddr);
				String dstIp = bytesToIpv6(dstAddr);

				context.setClientNode(new Node(srcIp, srcPort));
				context.setProxyClientNode(new Node(dstIp, dstPort));
			} else {
				throw new TioDecodeException("unsupported v2 protocol for IPv6: " + proto);
			}
		} else if (addrFamily == V2_AF_UNIX) {
			if (proto == V2_PROTO_STREAM || proto == V2_PROTO_DGRAM) {
				// UNIX: 108+108 = 216 bytes
				if (addrLen < V2_ADDR_LEN_UNIX) {
					throw new TioDecodeException("invalid v2 unix address length: " + addrLen);
				}
				byte[] srcAddr = new byte[108];
				byte[] dstAddr = new byte[108];
				buffer.get(srcAddr);
				buffer.get(dstAddr);

				String srcPath = new String(srcAddr, StandardCharsets.US_ASCII).trim();
				String dstPath = new String(dstAddr, StandardCharsets.US_ASCII).trim();

				context.setClientNode(new Node(srcPath, 0));
				context.setProxyClientNode(new Node(dstPath, 0));
			} else {
				throw new TioDecodeException("unsupported v2 protocol for UNIX: " + proto);
			}
		} else if (addrFamily == V2_AF_UNSPEC) {
			// UNSPEC: 跳过地址信息，不设置节点
			if (addrLen > 0) {
				ByteBufferUtil.skipBytes(buffer, addrLen);
			}
		} else {
			throw new TioDecodeException("unsupported v2 address family: " + addrFamily);
		}
	}

	/**
	 * 将字节数组转换为 IPv4 地址字符串
	 */
	private static String bytesToIp(byte[] ip) {
		// 最大长度 xxx.xxx.xxx.xxx
		return String.valueOf(ip[0] & 0xFF) +
			'.' + (ip[1] & 0xFF) +
			'.' + (ip[2] & 0xFF) +
			'.' + (ip[3] & 0xFF);
	}

	/**
	 * 将字节数组转换为 IPv6 地址字符串 (标准格式，小写)
	 */
	private static String bytesToIpv6(byte[] ip) {
		StringBuilder sb = new StringBuilder(39);
		for (int i = 0; i < 8; i++) {
			if (i > 0) {
				sb.append(':');
			}
			int val = ((ip[i * 2] & 0xFF) << 8) | (ip[i * 2 + 1] & 0xFF);
			sb.append(Integer.toHexString(val));
		}
		return sb.toString();
	}

	/**
	 * 解码 V2 proxy protocol (for test)
	 *
	 * @param buffer         ByteBuffer
	 * @param readableLength readableLength
	 * @return ProxyProtocolMessage
	 * @throws TioDecodeException TioDecodeException
	 */
	public static ProxyProtocolMessage decodeV2ForTest(ByteBuffer buffer, int readableLength) throws TioDecodeException {
		if (readableLength < V2_HEADER_LENGTH) {
			throw new TioDecodeException("insufficient data for v2 header, need at least " + V2_HEADER_LENGTH + " bytes");
		}

		// 检测签名
		byte[] sig = new byte[12];
		buffer.get(sig);
		if (!Arrays.equals(sig, V2_SIGNATURE)) {
			throw new TioDecodeException("invalid v2 signature");
		}

		// 读取版本和命令
		byte verCmd = buffer.get();
		int version = (verCmd & 0xFF) >>> 4;
		int cmd = verCmd & 0x0F;

		if (version != 2) {
			throw new TioDecodeException("invalid v2 proxy protocol version: " + version);
		}

		// 读取地址族和协议
		byte fam = buffer.get();

		// 读取地址长度 (网络字节序 = 大端)
		short addrLen = ByteBufferUtil.readShortBE(buffer);

		// 根据命令处理
		if (cmd == V2_CMD_LOCAL) {
			// LOCAL: 返回空消息
			return new ProxyProtocolMessage("LOCAL", null, null, 0, 0);
		} else if (cmd == V2_CMD_PROXY) {
			// PROXY: 解析地址
			return decodeV2AddressForTest(buffer, fam, addrLen);
		} else {
			throw new TioDecodeException("invalid v2 proxy protocol command: " + cmd);
		}
	}

	/**
	 * 解码 V2 地址 (for test)
	 */
	private static ProxyProtocolMessage decodeV2AddressForTest(ByteBuffer buffer, byte fam, int addrLen) throws TioDecodeException {
		int addrFamily = fam & 0xFF & 0xF0;
		int proto = fam & 0x0F;

		if (addrFamily == V2_AF_INET) {
			if (proto == V2_PROTO_STREAM || proto == V2_PROTO_DGRAM) {
				if (addrLen < V2_ADDR_LEN_IPV4 || buffer.remaining() < addrLen) {
					throw new TioDecodeException("invalid v2 ipv4 address length: " + addrLen);
				}
				byte[] srcAddr = new byte[4];
				byte[] dstAddr = new byte[4];
				buffer.get(srcAddr);
				buffer.get(dstAddr);
				int srcPort = ((buffer.get() & 0xFF) << 8) | (buffer.get() & 0xFF);
				int dstPort = ((buffer.get() & 0xFF) << 8) | (buffer.get() & 0xFF);

				String protocol = (proto == V2_PROTO_STREAM) ? "TCP4" : "UDP4";
				return new ProxyProtocolMessage(protocol, bytesToIp(srcAddr), bytesToIp(dstAddr), srcPort, dstPort);
			} else {
				throw new TioDecodeException("unsupported v2 protocol for IPv4: " + proto);
			}
		} else if (addrFamily == V2_AF_INET6) {
			if (proto == V2_PROTO_STREAM || proto == V2_PROTO_DGRAM) {
				if (addrLen < V2_ADDR_LEN_IPV6 || buffer.remaining() < addrLen) {
					throw new TioDecodeException("invalid v2 ipv6 address length: " + addrLen);
				}
				byte[] srcAddr = new byte[16];
				byte[] dstAddr = new byte[16];
				buffer.get(srcAddr);
				buffer.get(dstAddr);
				int srcPort = ((buffer.get() & 0xFF) << 8) | (buffer.get() & 0xFF);
				int dstPort = ((buffer.get() & 0xFF) << 8) | (buffer.get() & 0xFF);

				String protocol = (proto == V2_PROTO_STREAM) ? "TCP6" : "UDP6";
				return new ProxyProtocolMessage(protocol, bytesToIpv6(srcAddr), bytesToIpv6(dstAddr), srcPort, dstPort);
			} else {
				throw new TioDecodeException("unsupported v2 protocol for IPv6: " + proto);
			}
		} else if (addrFamily == V2_AF_UNIX) {
			throw new TioDecodeException("unsupported v2 address family: UNIX (not typically used in tests)");
		} else if (addrFamily == V2_AF_UNSPEC) {
			return new ProxyProtocolMessage("UNSPEC", null, null, 0, 0);
		} else {
			throw new TioDecodeException("unsupported v2 address family: " + addrFamily);
		}
	}

}
