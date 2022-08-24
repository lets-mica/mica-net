package org.tio.core.tcp;

import org.tio.core.intf.EncodedPacket;
import org.tio.core.intf.Packet;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 示例：长度固定的解码器
 *
 * @author L.cm
 */
public class FixedLengthCodec {
	/**
	 * 示例：默认的消息长度
	 */
	public static FixedLengthCodec INSTANCE = new FixedLengthCodec("mica:166130695837".getBytes(StandardCharsets.UTF_8).length);

	private final int length;

	public FixedLengthCodec(int length) {
		this.length = length + 1;
	}

	public Packet decode(ByteBuffer buffer, int readableLength) {
		if (readableLength < length) {
			return null;
		}
		byte[] bytes = new byte[length];
		buffer.get(bytes);
		return new EncodedPacket(bytes);
	}

	public ByteBuffer encode(Packet packet) {
		if (packet instanceof EncodedPacket) {
			return ByteBuffer.wrap(((EncodedPacket) packet).getBytes());
		}
		throw new IllegalArgumentException("is not EncodedPacket");
	}

}
