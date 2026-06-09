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
		// PROXY TCP4 192.168.0.1 192.168.0.11 56324 443\r\n
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
		// 检测签名
		byte[] sig = ByteBufferUtil.readBytes(buffer, 12);
		if (!Arrays.equals(sig, V2_SIGNATURE)) {
			throw new TioDecodeException("invalid v2 signature");
		}
		return parseV2Body(buffer, readableLength - V2_HEADER_LENGTH, null);
	}

	// ==================== 统一解析核心 ====================

	/**
	 * 在 buffer 上原地解析代理头；成功时推进 position 并写入 context。
	 *
	 * @param context        连接上下文
	 * @param buffer         待解析数据（position 指向起始位置）
	 * @param readableLength 可读字节数
	 */
	private static ParseResult parseInPlace(ChannelContext context, ByteBuffer buffer, int readableLength) throws TioDecodeException {
		// 至少需要 6 字节才能判定 V1 前缀
		if (readableLength < V1_MIN_HEAD_LENGTH) {
			return ParseResult.needMore();
		}
		int startPos = buffer.position();
		// V2：完整签名或半包前缀均走 V2 路径。
		// HAProxy 默认 send-proxy-v2，TCP 拆包时可能先到达不足 16 字节的 v2 头；
		// 若误判为 NOT_PROXY 会把 v2 二进制头喂给 SSL，握手永久失败。
		if (looksLikeV2(buffer, readableLength)) {
			if (readableLength < V2_HEADER_LENGTH) {
				return ParseResult.needMore();
			}
			return parseV2InPlace(context, buffer, readableLength, startPos);
		}
		// V1: PROXY TCP4 192.168.0.1 192.168.0.11 56324 443\r\n
		buffer.mark();
		String proxyPrefix = ByteBufferUtil.readString(buffer, V1_MIN_HEAD_LENGTH, StandardCharsets.US_ASCII);
		if (!"PROXY ".equals(proxyPrefix)) {
			// 非 PROXY 协议，清除 key 并回退
			buffer.reset();
			removeProxyProtocol(context);
			return ParseResult.notProxy();
		}
		ProxyProtocolMessage message = parseV1Message(buffer, readableLength);
		if (message == null) {
			// 半包（如 \r\n 未到达），恢复 position 等待更多数据
			buffer.reset();
			return ParseResult.needMore();
		}
		removeProxyProtocol(context);
		applyMessage(context, message);
		return ParseResult.parsed(buffer.position() - startPos);
	}

	/**
	 * 原地解析 V2 代理头。
	 */
	private static ParseResult parseV2InPlace(ChannelContext context, ByteBuffer buffer, int readableLength, int startPos) throws TioDecodeException {
		if (readableLength < V2_HEADER_LENGTH) {
			return ParseResult.needMore();
		}
		buffer.mark();
		// 跳过 12 字节签名
		ByteBufferUtil.skipBytes(buffer, 12);
		// 读取版本和命令
		byte verCmd = buffer.get();
		byte version = (byte) ((verCmd & 0xF0) >> 4);
		byte cmd = (byte) (verCmd & 0x0F);
		if (version != 2) {
			throw new TioDecodeException("invalid v2 proxy protocol version: " + version);
		}
		// 读取地址族和协议
		byte fam = buffer.get();
		// 读取地址长度（网络字节序 = 大端）
		short addrLen = ByteBufferUtil.readShortBE(buffer);
		// 检查数据完整性: 16(header) + addrLen
		int totalLength = V2_HEADER_LENGTH + (addrLen & 0xFFFF);
		if (readableLength < totalLength) {
			buffer.reset();
			// 包长度不够，等待更多数据
			return ParseResult.needMore();
		}
		ProxyProtocolMessage message;
		if (cmd == V2_CMD_LOCAL) {
			// LOCAL: 跳过地址信息，不设置节点
			message = new ProxyProtocolMessage("LOCAL", null, null, 0, 0);
			ByteBufferUtil.skipBytes(buffer, addrLen & 0xFFFF);
		} else if (cmd == V2_CMD_PROXY) {
			// PROXY: 解析地址
			int addrStart = buffer.position();
			message = parseV2AddressMessage(buffer, fam, addrLen & 0xFFFF);
			// addrLen 可能大于实际地址块，跳过剩余填充字节
			int addrRemaining = (addrLen & 0xFFFF) - (buffer.position() - addrStart);
			if (addrRemaining > 0) {
				ByteBufferUtil.skipBytes(buffer, addrRemaining);
			}
		} else {
			throw new TioDecodeException("invalid v2 proxy protocol command: " + cmd);
		}
		// 跳过 TLV 扩展（如果有）
		int tlvsLength = readableLength - totalLength;
		if (tlvsLength > 0) {
			ByteBufferUtil.skipBytes(buffer, tlvsLength);
		}
		removeProxyProtocol(context);
		if (cmd == V2_CMD_PROXY) {
			applyMessage(context, message);
		}
		return ParseResult.parsed(buffer.position() - startPos);
	}

	/**
	 * 解析 V2 地址块（buffer 当前位置在 verCmd 之后，或 fam/addrLen 之后）。
	 *
	 * @param buffer     数据缓冲区
	 * @param bodyLength 签名之后的剩余可读长度（生产路径已由 parseV2InPlace 消费 fam/addrLen）
	 * @param fam        地址族字节；为 null 时从 buffer 读取（测试路径签名后尚未读 fam/addrLen）
	 */
	private static ProxyProtocolMessage parseV2Body(ByteBuffer buffer, int bodyLength, Byte fam) throws TioDecodeException {
		byte famByte;
		int addrLen;
		if (fam == null) {
			// 测试路径：decodeV2ForTest 已消费签名，此处继续读头部
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

	/**
	 * 解析 V1 消息体（调用前 buffer 已消费 "PROXY " 前缀）。
	 *
	 * @return 完整消息；半包时返回 null
	 */
	private static ProxyProtocolMessage parseV1Message(ByteBuffer buffer, int readableLength) throws TioDecodeException {
		int endOfLine = findEndOfLine(buffer);
		// 超长可能是半包多次进入，也可能是恶意数据
		if (endOfLine > V1_MAX_LENGTH || (readableLength > V1_MAX_LENGTH && endOfLine == -1)) {
			throw new TioDecodeException("Error v1 proxy protocol, readableLength: " + readableLength);
		}
		if (endOfLine == -1) {
			// 半包，\r\n 未到达
			return null;
		}
		// PROXY TCP4 ... \r\n → 去除前缀后: TCP4 192.168.0.1 192.168.0.11 56324 443
		String header = ByteBufferUtil.readString(buffer, endOfLine - buffer.position(), StandardCharsets.US_ASCII);
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
	 * 解析 V2 地址块并构造消息（生产/测试共用）。
	 */
	private static ProxyProtocolMessage parseV2AddressMessage(ByteBuffer buffer, byte fam, int addrLen) throws TioDecodeException {
		int addrFamily = fam & 0xFF & 0xF0; // 高 4 位：地址族
		int proto = fam & 0x0F;             // 低 4 位：传输协议

		if (addrFamily == V2_AF_INET) {
			if (proto != V2_PROTO_STREAM && proto != V2_PROTO_DGRAM) {
				throw new TioDecodeException("unsupported v2 protocol for IPv4: " + proto);
			}
			if (addrLen < V2_ADDR_LEN_IPV4 || buffer.remaining() < addrLen) {
				throw new TioDecodeException("invalid v2 ipv4 address length: " + addrLen);
			}
			// IPv4: 4+4+2+2 = 12 bytes
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
			// IPv6: 16+16+2+2 = 36 bytes
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
			// UNIX: 108+108 = 216 bytes
			byte[] srcAddr = new byte[108];
			byte[] dstAddr = new byte[108];
			buffer.get(srcAddr);
			buffer.get(dstAddr);
			String srcPath = new String(srcAddr, StandardCharsets.US_ASCII).trim();
			String dstPath = new String(dstAddr, StandardCharsets.US_ASCII).trim();
			return new ProxyProtocolMessage("UNIX", srcPath, dstPath, 0, 0);
		}
		if (addrFamily == V2_AF_UNSPEC) {
			// UNSPEC: 跳过地址信息，不设置节点
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

	/**
	 * 将解析结果写入 ChannelContext（设置真实客户端地址）。
	 */
	private static void applyMessage(ChannelContext context, ProxyProtocolMessage message) {
		String protocol = message.getProtocol();
		if (UNKNOWN.equals(protocol)) {
			// UNKNOWN 协议按规范丢弃其他头字段，仅标记代理节点
			context.setProxyClientNode(new Node(UNKNOWN, message.getDestinationPort()));
			return;
		}
		if ("LOCAL".equals(protocol) || "UNSPEC".equals(protocol)) {
			// LOCAL/UNSPEC 不携带地址信息
			return;
		}
		context.setClientNode(new Node(message.getSourceAddress(), message.getSourcePort()));
		context.setProxyClientNode(new Node(message.getDestinationAddress(), message.getDestinationPort()));
	}

	/**
	 * Proxy protocol message for 'UNKNOWN' proxied protocols. Per spec, when the proxied protocol is
	 * 'UNKNOWN' we must discard all other header values.
	 */
	private static ProxyProtocolMessage unknownMsg() {
		return new ProxyProtocolMessage(UNKNOWN, null, null, 0, 0);
	}

	/**
	 * 查找 \r\n 结束位置。
	 *
	 * @return \r 的字节索引；未找到返回 -1
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

	/**
	 * 检测 V2 签名（不推进 buffer position，避免 mark/reset 开销）。
	 */
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

	/**
	 * 判断是否像 V2 代理头（含 TCP 半包：可读字节为 v2 签名的前缀也算）。
	 */
	private static boolean looksLikeV2(ByteBuffer buffer, int readableLength) {
		int pos = buffer.position();
		int checkLen = Math.min(readableLength, V2_SIGNATURE.length);
		for (int i = 0; i < checkLen; i++) {
			if (buffer.get(pos + i) != V2_SIGNATURE[i]) {
				return false;
			}
		}
		return checkLen > 0;
	}

	/**
	 * 将字节数组转换为 IPv4 地址字符串。
	 */
	private static String bytesToIp(byte[] ip) {
		// 最大长度 xxx.xxx.xxx.xxx
		return String.valueOf(ip[0] & 0xFF) +
			'.' + (ip[1] & 0xFF) +
			'.' + (ip[2] & 0xFF) +
			'.' + (ip[3] & 0xFF);
	}

	/**
	 * 将字节数组转换为 IPv6 地址字符串（标准格式，小写）。
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
	 * 代理协议解析状态与结果（{@link PreParser#feed} 与 {@link #parseInPlace} 共用）。
	 */
	public static final class ParseResult {
		/**
		 * 解析状态。
		 */
		public enum State {
			/** 半包，需要更多数据 */
			NEED_MORE,
			/** 不是代理头，data 是全部数据 */
			NOT_PROXY,
			/** 代理头解析成功，data 是代理头之后的剩余数据（可能为空） */
			PARSED,
			/** 解析异常，data 是全部数据，error 是异常 */
			ERROR
		}

		/** 单例 NEED_MORE 结果，避免重复创建 */
		private static final ParseResult NEED_MORE_INSTANCE = new ParseResult(State.NEED_MORE, null, 0, null);

		public final State state;
		/** 待路由的数据（NEED_MORE 时为 null） */
		public final ByteBuffer data;
		/** 已消费的字节数（parseInPlace 产出；仅 PARSED 时有意义） */
		public final int consumed;
		/** 异常（仅 ERROR 时有值） */
		public final Throwable error;

		private ParseResult(State state, ByteBuffer data, int consumed, Throwable error) {
			this.state = state;
			this.data = data;
			this.consumed = consumed;
			this.error = error;
		}

		static ParseResult needMore() {
			return NEED_MORE_INSTANCE;
		}

		static ParseResult notProxy() {
			return new ParseResult(State.NOT_PROXY, null, 0, null);
		}

		static ParseResult parsed(int consumed) {
			return new ParseResult(State.PARSED, null, consumed, null);
		}

		static ParseResult of(State state, ByteBuffer data, Throwable error) {
			return new ParseResult(state, data, 0, error);
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
		/** 半包累积缓冲区；null 表示尚未开始累积或已解析完成 */
		private ByteBuffer buffer;

		public PreParser(ChannelContext context) {
			this(context, DEFAULT_INITIAL_SIZE, DEFAULT_MAX_SIZE);
		}

		public PreParser(ChannelContext context, int initialSize, int maxSize) {
			this.context = context;
			this.initialSize = initialSize;
			this.maxSize = maxSize;
		}

		/**
		 * 判断是否仍在累积（半包等待中）。
		 */
		public boolean isActive() {
			return buffer != null;
		}

		/**
		 * 喂入新数据并尝试解析代理头。
		 *
		 * @param newData 新到的数据（方法会消费其中能装入累积缓冲区的部分）
		 * @return 解析结果，调用方根据 {@link ParseResult#state} 决定路由
		 */
		public ParseResult feed(ByteBuffer newData) {
			// 1) 累积
			if (buffer == null) {
				buffer = ByteBuffer.allocate(initialSize);
			}
			int toCopy = Math.min(newData.remaining(), buffer.remaining());
			if (toCopy < newData.remaining()) {
				// 缓冲区装不下当前数据
				int needed = buffer.position() + newData.remaining();
				if (needed > maxSize) {
					// 累积超过最大尺寸仍无法识别为 PROXY 头，回退（合并所有数据）
					ByteBuffer combined = combineBuffers(buffer, newData);
					buffer = null;
					removeProxyProtocol(context);
					return ParseResult.of(ParseResult.State.NOT_PROXY, combined, null);
				}
				// 扩大缓冲区
				int newCapacity = Math.min(Math.max(buffer.capacity() * 2, needed), maxSize);
				ByteBuffer newBuf = ByteBuffer.allocate(newCapacity);
				buffer.flip();
				newBuf.put(buffer);
				buffer = newBuf;
				toCopy = Math.min(newData.remaining(), buffer.remaining());
			}
			if (toCopy > 0) {
				// 只拷贝能装入的部分，保留 newData 剩余供后续 combineBuffers 使用
				int oldLimit = newData.limit();
				newData.limit(newData.position() + toCopy);
				buffer.put(newData);
				newData.limit(oldLimit);
			}

			// 2) 准备解析
			buffer.flip();
			int readableLength = buffer.remaining();
			if (readableLength < V1_MIN_HEAD_LENGTH) {
				prepareForMore();
				return ParseResult.needMore();
			}

			// 3) 原地解析（无拷贝、无 decode 回调链）
			try {
				ParseResult parse = parseInPlace(context, buffer, readableLength);
				switch (parse.state) {
					case NEED_MORE:
						// 半包（V1 \r\n 未到达 / V2 长度不够等）
						prepareForMore();
						return ParseResult.needMore();
					case NOT_PROXY:
						// 不是 PROXY 头，全部数据回退
						ByteBuffer allData = ByteBufferUtil.copy(buffer);
						buffer = null;
						return ParseResult.of(ParseResult.State.NOT_PROXY, allData, null);
					case PARSED:
						// 代理头解析成功，剩余数据交给调用方（需拷贝，readByteBuffer 会被复用）
						ByteBuffer remaining = buffer.hasRemaining()
							? ByteBufferUtil.copy(buffer)
							: ByteBuffer.allocate(0);
						buffer = null;
						return ParseResult.of(ParseResult.State.PARSED, remaining, null);
					default:
						throw new IllegalStateException("unknown parse state: " + parse.state);
				}
			} catch (TioDecodeException e) {
				// 解析异常，合并所有数据并回退
				prepareForMore();
				ByteBuffer combined = combineBuffers(buffer, newData);
				buffer = null;
				removeProxyProtocol(context);
				return ParseResult.of(ParseResult.State.ERROR, combined, e);
			}
		}

		/**
		 * 切换为写模式，继续累积下一批数据。
		 */
		private void prepareForMore() {
			buffer.position(buffer.limit());
			buffer.limit(buffer.capacity());
		}

		/**
		 * 合并累积缓冲区和当前 newData 剩余数据。
		 */
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
	}

}
