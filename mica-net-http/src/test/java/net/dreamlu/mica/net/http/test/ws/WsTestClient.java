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

package net.dreamlu.mica.net.http.test.ws;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * WebSocket 测试客户端，用裸 socket 拼协议，方便构造各种非法/边界帧。
 *
 * @author L.cm
 */
public class WsTestClient implements AutoCloseable {

	/**
	 * 读到的帧
	 */
	public static class Frame {
		public final boolean fin;
		public final int opcode;
		public final byte[] payload;

		public Frame(boolean fin, int opcode, byte[] payload) {
			this.fin = fin;
			this.opcode = opcode;
			this.payload = payload;
		}

		public String text() {
			return new String(payload, StandardCharsets.UTF_8);
		}
	}

	private final Socket socket;
	private final DataInputStream in;
	private final OutputStream out;
	/**
	 * 握手响应状态行，例如：HTTP/1.1 101 Switching Protocols
	 */
	private String statusLine;
	/**
	 * 握手响应头
	 */
	private final java.util.Map<String, String> headers = new java.util.HashMap<>();

	public WsTestClient(int port) throws IOException {
		this.socket = new Socket("127.0.0.1", port);
		this.socket.setSoTimeout(5000);
		this.in = new DataInputStream(socket.getInputStream());
		this.out = socket.getOutputStream();
	}

	/**
	 * 发送一个规范的握手请求
	 *
	 * @throws IOException IOException
	 */
	public void handshake() throws IOException {
		handshake("GET /ws HTTP/1.1", "Upgrade: websocket\r\nConnection: Upgrade\r\n"
			+ "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\nSec-WebSocket-Version: 13\r\n");
	}

	/**
	 * 发送自定义的握手请求
	 *
	 * @param requestLine  请求行
	 * @param headers      请求头
	 * @throws IOException IOException
	 */
	public void handshake(String requestLine, String headers) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(requestLine).append("\r\n");
		sb.append("Host: 127.0.0.1\r\n");
		sb.append(headers);
		sb.append("\r\n");
		out.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
		out.flush();
		readHttpResponse();
	}

	/**
	 * 读取 http 响应（状态行 + 头）
	 *
	 * @throws IOException IOException
	 */
	public void readHttpResponse() throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int b;
		// 读到空行结束
		int contentLength = -1;
		while ((b = in.read()) != -1) {
			buffer.write(b);
			if (b == '\n') {
				String line = new String(buffer.toByteArray(), StandardCharsets.US_ASCII);
				int idx = line.length() - 2;
				String trimmed = line.substring(0, Math.max(idx, 0)).trim();
				if (trimmed.isEmpty()) {
					break;
				}
				if (statusLine == null) {
					statusLine = trimmed;
				} else {
					int colon = trimmed.indexOf(':');
					if (colon > 0) {
						headers.put(trimmed.substring(0, colon).trim().toLowerCase(), trimmed.substring(colon + 1).trim());
					}
				}
				String lower = trimmed.toLowerCase();
				if (lower.startsWith("content-length:")) {
					contentLength = Integer.parseInt(trimmed.substring(trimmed.indexOf(':') + 1).trim());
				}
				buffer.reset();
			}
		}
		if (contentLength > 0) {
			byte[] body = new byte[contentLength];
			in.readFully(body);
		}
	}

	public String getStatusLine() {
		return statusLine;
	}

	public int getStatusCode() {
		if (statusLine == null) {
			return -1;
		}
		String[] parts = statusLine.split(" ");
		return parts.length > 1 ? Integer.parseInt(parts[1]) : -1;
	}

	public String getHeader(String name) {
		return headers.get(name.toLowerCase());
	}

	/**
	 * 发送一个帧
	 *
	 * @param opcode 操作码
	 * @param payload 负载
	 * @param fin     是否是最后一帧
	 * @param masked  是否带掩码
	 * @throws IOException IOException
	 */
	public void sendFrame(int opcode, byte[] payload, boolean fin, boolean masked) throws IOException {
		out.write(buildFrame(opcode, payload, fin, masked));
		out.flush();
	}

	public void sendText(String text) throws IOException {
		sendFrame(0x1, text.getBytes(StandardCharsets.UTF_8), true, true);
	}

	public void sendPing(byte[] data) throws IOException {
		sendFrame(0x9, data, true, true);
	}

	public void sendClose(int closeCode, String reason) throws IOException {
		byte[] payload = new byte[2 + (reason == null ? 0 : reason.getBytes(StandardCharsets.UTF_8).length)];
		payload[0] = (byte) ((closeCode >> 8) & 0xFF);
		payload[1] = (byte) (closeCode & 0xFF);
		if (reason != null) {
			System.arraycopy(reason.getBytes(StandardCharsets.UTF_8), 0, payload, 2, payload.length - 2);
		}
		sendFrame(0x8, payload, true, true);
	}

	/**
	 * 读取一个帧
	 *
	 * @return 帧，连接已关闭时返回 null
	 * @throws IOException IOException
	 */
	public Frame readFrame() throws IOException {
		int b0 = in.read();
		if (b0 == -1) {
			return null;
		}
		int b1 = in.read();
		if (b1 == -1) {
			return null;
		}
		boolean fin = (b0 & 0x80) != 0;
		int opcode = b0 & 0x0F;
		boolean masked = (b1 & 0x80) != 0;
		long length = b1 & 0x7F;
		if (length == 126) {
			length = in.readUnsignedShort();
		} else if (length == 127) {
			length = in.readLong();
		}
		byte[] mask = null;
		if (masked) {
			mask = new byte[4];
			in.readFully(mask);
		}
		byte[] payload = new byte[(int) length];
		in.readFully(payload);
		if (masked) {
			for (int i = 0; i < payload.length; i++) {
				payload[i] = (byte) (payload[i] ^ mask[i % 4]);
			}
		}
		return new Frame(fin, opcode, payload);
	}

	/**
	 * 判断服务端是否已经关闭了连接
	 *
	 * @return true 表示连接已被服务端关闭
	 * @throws IOException IOException
	 */
	public boolean isClosedByServer() throws IOException {
		try {
			return in.read() == -1;
		} catch (java.net.SocketException e) {
			return true;
		}
	}

	/**
	 * 构造一个帧
	 *
	 * @param opcode  操作码
	 * @param payload 负载
	 * @param fin     是否是最后一帧
	 * @param masked  是否带掩码
	 * @return 帧的字节数据
	 */
	public static byte[] buildFrame(int opcode, byte[] payload, boolean fin, boolean masked) {
		int length = payload.length;
		int headerLength = 2;
		if (length >= 65536) {
			headerLength += 8;
		} else if (length >= 126) {
			headerLength += 2;
		}
		if (masked) {
			headerLength += 4;
		}
		java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(headerLength + length);
		buf.put((byte) ((fin ? 0x80 : 0x00) | (opcode & 0x0F)));
		int maskBit = masked ? 0x80 : 0x00;
		if (length < 126) {
			buf.put((byte) (maskBit | length));
		} else if (length <= 0xFFFF) {
			buf.put((byte) (maskBit | 126));
			buf.putShort((short) length);
		} else {
			buf.put((byte) (maskBit | 127));
			buf.putLong(length);
		}
		byte[] data = payload;
		if (masked) {
			byte[] mask = {0x12, 0x34, 0x56, 0x78};
			buf.put(mask);
			data = payload.clone();
			for (int i = 0; i < data.length; i++) {
				data[i] = (byte) (data[i] ^ mask[i % 4]);
			}
		}
		buf.put(data);
		return buf.array();
	}

	/**
	 * 生成一个随机的 Sec-WebSocket-Key
	 *
	 * @return base64 编码的 key
	 */
	public static String randomKey() {
		byte[] key = new byte[16];
		new java.security.SecureRandom().nextBytes(key);
		return Base64.getEncoder().encodeToString(key);
	}

	@Override
	public void close() {
		try {
			socket.close();
		} catch (IOException e) {
			// ignore
		}
	}
}
