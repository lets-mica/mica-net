package org.tio.core.agnss;

import org.tio.core.intf.EncodedPacket;
import org.tio.core.intf.Packet;
import org.tio.utils.buffer.ByteBufferUtil;

import java.nio.ByteBuffer;

public class AGNSSCodec {

	public Packet decode(ByteBuffer buffer, int readableLength) {
		byte[] bytes = ByteBufferUtil.readBytes(buffer, readableLength);
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
