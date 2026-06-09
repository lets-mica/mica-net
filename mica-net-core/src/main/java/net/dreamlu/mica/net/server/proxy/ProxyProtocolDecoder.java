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
import net.dreamlu.mica.net.utils.buffer.ByteBufferUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 开启 nginx 代理协议时需要开启，转发代理 ip 信息。
 * <p>
 * proxy_protocol on;
 * </p>
 * <p>
 * I/O 读路径使用 {@link PreParser#feed(ByteBuffer)} 在累积缓冲区上原地解析代理头，
 * 典型用法见 {@link net.dreamlu.mica.net.core.ReadCompletionHandler}。
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
	private static final byte[] V2_SIGNATURE = new byte[]{
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
	private static final byte V2_PROTO_STREAM = 0x01;  // TCP/SOCK_STREAM
	private static final byte V2_PROTO_DGRAM = 0x02;    // UDP/SOCK_DGRAM

	// V2 地址长度
	private static final int V2_ADDR_LEN_IPV4 = 12;  // 4+4+2+2
	private static final int V2_ADDR_LEN_IPV6 = 36;  // 16+16+2+2
	private static final int V2_ADDR_LEN_UNIX = 216; // 108+108

	/**
	 * 默认初始缓冲区大小（V1 最大 108 字节，V2 含 TLV 可达 64K+，4096 覆盖绝大多数场景）
	 */
	public static final int DEFAULT_INITIAL_SIZE = 4096;
	/**
	 * 默认最大缓冲区大小（64KB），超过此尺寸仍无法解析则回退
	 */
	public static final int DEFAULT_MAX_SIZE = 64 * 1024;

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
	 * 解码 V1 proxy protocol（测试用）
	 *
	 * @param buffer         ByteBuffer
	 * @param readableLength readableLength
	 * @return ProxyProtocolMessage
	 * @throws TioDecodeException TioDecodeException
	 */
	public static ProxyProtocolMessage decodeForTest(ByteBuffer buffer, int readableLength) throws TioDecodeException {
		String proxyPrefix = ByteBufferUtil.readString(buffer, V1_MIN_HEAD_LENGTH, StandardCharsets.US_ASCII);
		if (!"PROXY ".equals(proxyPrefix)) {
			throw new TioDecodeException("unknown identifier: " + proxyPrefix);
		}
		return parseV1Message(buffer, readableLength);
	}

	/**
	 * 解码 V2 proxy protocol（测试用）
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
		byte[] sig = ByteBufferUtil.readBytes(buffer, 12);
		if (!Arrays.equals(sig, V2_SIGNATURE)) {
			throw new TioDecodeException("invalid v2 signature");
		}
		return parseV2Body(buffer, readableLength - V2_HEADER_LENGTH, null);
	}

	// ==================== 统一解析核心 ====================

	/**
	 * 在 buffer 上原地解析代理头；成功时推进 position 并写入 context。
	 */
	private static HeaderParse parseInPlace(ChannelContext context, ByteBuffer buffer, int readableLength) throws TioDecodeException {
		if (readableLength < V1_MIN_HEAD_LENGTH) {
			return HeaderParse.needMore();
		}
		int startPos = buffer.position();
		if (readableLength >= V2_MIN_HEAD_LENGTH && isV2Signature(buffer)) {
			return parseV2InPlace(context, buffer, readableLength, startPos);
		}
		buffer.mark();
		String proxyPrefix = ByteBufferUtil.readString(buffer, V1_MIN_HEAD_LENGTH, StandardCharsets.US_ASCII);
		if (!"PROXY ".equals(proxyPrefix)) {
			buffer.reset();
			removeProxyProtocol(context);
			return HeaderParse.notProxy();
		}
		ProxyProtocolMessage message = parseV1Message(buffer, readableLength);
		if (message == null) {
			buffer.reset();
			return HeaderParse.needMore();
		}
		removeProxyProtocol(context);
		applyMessage(context, message);
		return HeaderParse.parsed(buffer.position() - startPos);
	}

	private static HeaderParse parseV2InPlace(ChannelContext context, ByteBuffer buffer, int readableLength, int startPos) throws TioDecodeException {
		if (readableLength < V2_HEADER_LENGTH) {
			return HeaderParse.needMore();
		}
		buffer.mark();
		ByteBufferUtil.skipBytes(buffer, 12);
		byte verCmd = buffer.get();
		byte version = (byte) ((verCmd & 0xF0) >> 4);
		byte cmd = (byte) (verCmd & 0x0F);
		if (version != 2) {
			throw new TioDecodeException("invalid v2 proxy protocol version: " + version);
		}
		byte fam = buffer.get();
		short addrLen = ByteBufferUtil.readShortBE(buffer);
		int totalLength = V2_HEADER_LENGTH + (addrLen & 0xFFFF);
		if (readableLength < totalLength) {
			buffer.reset();
			return HeaderParse.needMore();
		}
		ProxyProtocolMessage message;
		if (cmd == V2_CMD_LOCAL) {
			message = new ProxyProtocolMessage("LOCAL", null, null, 0, 0);
			ByteBufferUtil.skipBytes(buffer, addrLen & 0xFFFF);
		} else if (cmd == V2_CMD_PROXY) {
			int addrStart = buffer.position();
			message = parseV2AddressMessage(buffer, fam, addrLen & 0xFFFF);
			int addrRemaining = (addrLen & 0xFFFF) - (buffer.position() - addrStart);
			if (addrRemaining > 0) {
				ByteBufferUtil.skipBytes(buffer, addrRemaining);
			}
		} else {
			throw new TioDecodeException("invalid v2 proxy protocol command: " + cmd);
		}
		int tlvsLength = readableLength - totalLength;
		if (tlvsLength > 0) {
			ByteBufferUtil.skipBytes(buffer, tlvsLength);
		}
		removeProxyProtocol(context);
		if (cmd == V2_CMD_PROXY) {
			applyMessage(context, message);
		}
		return HeaderParse.parsed(buffer.position() - startPos);
	}

	/**
	 * 解析 V2 地址块（buffer 当前位置在 fam/addrLen 之后）。
	 *
	 * @param fam 地址族字节；为 null 时从 buffer 读取（测试路径已在签名后消费 fam/addrLen）
	 */
	private static ProxyProtocolMessage parseV2Body(ByteBuffer buffer, int bodyLength, Byte fam) throws TioDecodeException {
		byte famByte;
		int addrLen;
		if (fam == null) {
			byte verCmd = buffer.get();
			byte version = (byte) ((verCmd & 0xF0) >> 4);
			byte cmd = (byte) (verCmd & 0x0F);
			if (version != 2) {
				throw new TioDecodeException("invalid v2 proxy protocol version: " + version);
			}
			if (cmd == V2_CMD_LOCAL) {
				return new ProxyProtocolMessage("LOCAL", null, null, 0, 0);
			}
			if (cmd != V2_CMD_PROXY) {
				throw new TioDecodeException("invalid v2 proxy protocol command: " + cmd);
			}
			famByte = buffer.get();
			addrLen = ByteBufferUtil.readShortBE(buffer) & 0xFFFF;
		} else {
			famByte = fam;
			addrLen = bodyLength;
		}
		return parseV2AddressMessage(buffer, famByte, addrLen);
	}

	private static ProxyProtocolMessage parseV1Message(ByteBuffer buffer, int readableLength) throws TioDecodeException {
		int endOfLine = findEndOfLine(buffer);
		if (endOfLine > V1_MAX_LENGTH || (readableLength > V1_MAX_LENGTH && endOfLine == -1)) {
			throw new TioDecodeException("Error v1 proxy protocol, readableLength: " + readableLength);
		}
		if (endOfLine == -1) {
			return null;
		}
		String header = ByteBufferUtil.readString(buffer, endOfLine - buffer.position(), StandardCharsets.US_ASCII);
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

	private static ProxyProtocolMessage parseV2AddressMessage(ByteBuffer buffer, byte fam, int addrLen) throws TioDecodeException {
		int addrFamily = fam & 0xFF & 0xF0;
		int proto = fam & 0x0F;

		if (addrFamily == V2_AF_INET) {
			if (proto != V2_PROTO_STREAM && proto != V2_PROTO_DGRAM) {
				throw new TioDecodeException("unsupported v2 protocol for IPv4: " + proto);
			}
			if (addrLen < V2_ADDR_LEN_IPV4 || buffer.remaining() < addrLen) {
				throw new TioDecodeException("invalid v2 ipv4 address length: " + addrLen);
			}
			byte[] srcAddr = new byte[4];
			byte[] dstAddr = new byte[4];
			buffer.get(srcAddr);
			buffer.get(dstAddr);
			int srcPort = ByteBufferUtil.readUnsignedShortBE(buffer);
			int dstPort = ByteBufferUtil.readUnsignedShortBE(buffer);
			String protocol = (proto == V2_PROTO_STREAM) ? "TCP4" : "UDP4";
			return new ProxyProtocolMessage(protocol, bytesToIp(srcAddr), bytesToIp(dstAddr), srcPort, dstPort);
		}
		if (addrFamily == V2_AF_INET6) {
			if (proto != V2_PROTO_STREAM && proto != V2_PROTO_DGRAM) {
				throw new TioDecodeException("unsupported v2 protocol for IPv6: " + proto);
			}
			if (addrLen < V2_ADDR_LEN_IPV6 || buffer.remaining() < addrLen) {
				throw new TioDecodeException("invalid v2 ipv6 address length: " + addrLen);
			}
			byte[] srcAddr = new byte[16];
			byte[] dstAddr = new byte[16];
			buffer.get(srcAddr);
			buffer.get(dstAddr);
			int srcPort = ByteBufferUtil.readUnsignedShortBE(buffer);
			int dstPort = ByteBufferUtil.readUnsignedShortBE(buffer);
			String protocol = (proto == V2_PROTO_STREAM) ? "TCP6" : "UDP6";
			return new ProxyProtocolMessage(protocol, bytesToIpv6(srcAddr), bytesToIpv6(dstAddr), srcPort, dstPort);
		}
		if (addrFamily == V2_AF_UNIX) {
			if (proto != V2_PROTO_STREAM && proto != V2_PROTO_DGRAM) {
				throw new TioDecodeException("unsupported v2 protocol for UNIX: " + proto);
			}
			if (addrLen < V2_ADDR_LEN_UNIX || buffer.remaining() < addrLen) {
				throw new TioDecodeException("invalid v2 unix address length: " + addrLen);
			}
			byte[] srcAddr = new byte[108];
			byte[] dstAddr = new byte[108];
			buffer.get(srcAddr);
			buffer.get(dstAddr);
			String srcPath = new String(srcAddr, StandardCharsets.US_ASCII).trim();
			String dstPath = new String(dstAddr, StandardCharsets.US_ASCII).trim();
			return new ProxyProtocolMessage("UNIX", srcPath, dstPath, 0, 0);
		}
		if (addrFamily == V2_AF_UNSPEC) {
			if (addrLen > 0 && buffer.remaining() < addrLen) {
				throw new TioDecodeException("invalid v2 unspec address length: " + addrLen);
			}
			if (addrLen > 0) {
				ByteBufferUtil.skipBytes(buffer, addrLen);
			}
			return new ProxyProtocolMessage("UNSPEC", null, null, 0, 0);
		}
		throw new TioDecodeException("unsupported v2 address family: " + addrFamily);
	}

	private static void applyMessage(ChannelContext context, ProxyProtocolMessage message) {
		String protocol = message.getProtocol();
		if (UNKNOWN.equals(protocol)) {
			context.setProxyClientNode(new Node(UNKNOWN, message.getDestinationPort()));
			return;
		}
		if ("LOCAL".equals(protocol) || "UNSPEC".equals(protocol)) {
			return;
		}
		context.setClientNode(new Node(message.getSourceAddress(), message.getSourcePort()));
		context.setProxyClientNode(new Node(message.getDestinationAddress(), message.getDestinationPort()));
	}

	private static ProxyProtocolMessage unknownMsg() {
		return new ProxyProtocolMessage(UNKNOWN, null, null, 0, 0);
	}

	private static int findEndOfLine(final ByteBuffer buffer) {
		final int n = buffer.limit();
		for (int i = buffer.position(); i < n; i++) {
			final byte b = buffer.get(i);
			if (b == '\r' && i < n - 1 && buffer.get(i + 1) == '\n') {
				return i;
			}
		}
		return -1;
	}

	private static boolean isV2Signature(ByteBuffer buffer) {
		if (buffer.remaining() < 12) {
			return false;
		}
		int pos = buffer.position();
		for (int i = 0; i < 12; i++) {
			if (buffer.get(pos + i) != V2_SIGNATURE[i]) {
				return false;
			}
		}
		return true;
	}

	private static String bytesToIp(byte[] ip) {
		return String.valueOf(ip[0] & 0xFF) +
			'.' + (ip[1] & 0xFF) +
			'.' + (ip[2] & 0xFF) +
			'.' + (ip[3] & 0xFF);
	}

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

	private static final class HeaderParse {
		enum Status {
			NEED_MORE, NOT_PROXY, PARSED
		}

		final Status status;
		final int consumed;

		private HeaderParse(Status status, int consumed) {
			this.status = status;
			this.consumed = consumed;
		}

		static HeaderParse needMore() {
			return new HeaderParse(Status.NEED_MORE, 0);
		}

		static HeaderParse notProxy() {
			return new HeaderParse(Status.NOT_PROXY, 0);
		}

		static HeaderParse parsed(int consumed) {
			return new HeaderParse(Status.PARSED, consumed);
		}
	}

	// ==================== PreParser ====================

	/**
	 * 代理头预解析器（用于在 SSL/业务解码前累积并解析代理头）。
	 * <p>
	 * 在累积缓冲区上原地调用 {@link #parseInPlace}，无额外拷贝、无 decode 回调链。
	 * </p>
	 * <p>
	 * 线程不安全，仅在 I/O 线程中使用。
	 * </p>
	 */
	public static final class PreParser {
		private final ChannelContext context;
		private final int initialSize;
		private final int maxSize;
		private ByteBuffer buffer;

		public PreParser(ChannelContext context) {
			this(context, DEFAULT_INITIAL_SIZE, DEFAULT_MAX_SIZE);
		}

		public PreParser(ChannelContext context, int initialSize, int maxSize) {
			this.context = context;
			this.initialSize = initialSize;
			this.maxSize = maxSize;
		}

		public boolean isActive() {
			return buffer != null;
		}

		public Result feed(ByteBuffer newData) {
			if (buffer == null) {
				buffer = ByteBuffer.allocate(initialSize);
			}
			int toCopy = Math.min(newData.remaining(), buffer.remaining());
			if (toCopy < newData.remaining()) {
				int needed = buffer.position() + newData.remaining();
				if (needed > maxSize) {
					ByteBuffer combined = combineBuffers(buffer, newData);
					buffer = null;
					removeProxyProtocol(context);
					return new Result(State.NOT_PROXY, combined, null);
				}
				int newCapacity = Math.min(Math.max(buffer.capacity() * 2, needed), maxSize);
				ByteBuffer newBuf = ByteBuffer.allocate(newCapacity);
				buffer.flip();
				newBuf.put(buffer);
				buffer = newBuf;
				toCopy = Math.min(newData.remaining(), buffer.remaining());
			}
			if (toCopy > 0) {
				int oldLimit = newData.limit();
				newData.limit(newData.position() + toCopy);
				buffer.put(newData);
				newData.limit(oldLimit);
			}

			buffer.flip();
			int readableLength = buffer.remaining();
			if (readableLength < V1_MIN_HEAD_LENGTH) {
				prepareForMore();
				return Result.NEED_MORE_INSTANCE;
			}

			try {
				HeaderParse parse = parseInPlace(context, buffer, readableLength);
				switch (parse.status) {
					case NEED_MORE:
						prepareForMore();
						return Result.NEED_MORE_INSTANCE;
					case NOT_PROXY:
						ByteBuffer allData = ByteBufferUtil.copy(buffer);
						buffer = null;
						return new Result(State.NOT_PROXY, allData, null);
					case PARSED:
						ByteBuffer remaining = buffer.hasRemaining()
							? ByteBufferUtil.copy(buffer)
							: ByteBuffer.allocate(0);
						buffer = null;
						return new Result(State.PARSED, remaining, null);
					default:
						throw new IllegalStateException("unknown parse status: " + parse.status);
				}
			} catch (TioDecodeException e) {
				prepareForMore();
				ByteBuffer combined = combineBuffers(buffer, newData);
				buffer = null;
				removeProxyProtocol(context);
				return new Result(State.ERROR, combined, e);
			}
		}

		private void prepareForMore() {
			buffer.position(buffer.limit());
			buffer.limit(buffer.capacity());
		}

		private static ByteBuffer combineBuffers(ByteBuffer accumulated, ByteBuffer newData) {
			accumulated.flip();
			int len1 = accumulated.remaining();
			int len2 = newData.remaining();
			ByteBuffer combined = ByteBuffer.allocate(len1 + len2);
			combined.put(accumulated);
			if (len2 > 0) {
				combined.put(newData);
			}
			combined.flip();
			return combined;
		}

		public enum State {
			NEED_MORE,
			PARSED,
			NOT_PROXY,
			ERROR
		}

		public static final class Result {
			private static final Result NEED_MORE_INSTANCE = new Result(State.NEED_MORE, null, null);

			public final State state;
			public final ByteBuffer data;
			public final Throwable error;

			private Result(State state, ByteBuffer data, Throwable error) {
				this.state = state;
				this.data = data;
				this.error = error;
			}
		}
	}

}
