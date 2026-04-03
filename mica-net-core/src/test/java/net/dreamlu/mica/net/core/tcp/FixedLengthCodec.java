package net.dreamlu.mica.net.core.tcp;

import net.dreamlu.mica.net.core.intf.EncodedPacket;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.utils.buffer.ByteBufferUtil;

import java.nio.ByteBuffer;

/**
 * 示例：长度固定的解码器
 *
 * @author L.cm
 */
public class FixedLengthCodec {

	private final int length;

	public FixedLengthCodec(int length) {
		this.length = length;
	}

	public Packet decode(ByteBuffer buffer, int readableLength) {
		if (readableLength < length) {
			return null;
		}
		byte[] bytes = new byte[length];
		buffer.get(bytes);
		// 方便 debug 调试
		System.out.println(ByteBufferUtil.toHexString(bytes));
		// 偷懒使用的 EncodedPacket，直接处理的 byte 数据，大家可以自定义编解码
		return new EncodedPacket(bytes);
	}

	public ByteBuffer encode(Packet packet) {
		// 偷懒使用的 EncodedPacket，直接处理的 byte 数据，大家可以自定义编解码
		if (packet instanceof EncodedPacket) {
			return ByteBuffer.wrap(((EncodedPacket) packet).getBytes());
		}
		throw new IllegalArgumentException("is not EncodedPacket");
	}

}
