package net.dreamlu.mica.net.core.tcp;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.TioConfig;
import net.dreamlu.mica.net.core.exception.TioDecodeException;
import net.dreamlu.mica.net.core.intf.EncodedPacket;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.server.intf.TioServerHandler;
import net.dreamlu.mica.net.utils.buffer.ByteBufferUtil;

import java.nio.ByteBuffer;

public class TestTioServerHandler implements TioServerHandler {
	private final FixedLengthCodec codec;

	public TestTioServerHandler(FixedLengthCodec codec) {
		this.codec = codec;
	}

	@Override
	public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext) throws TioDecodeException {
		return codec.decode(buffer, readableLength);
	}

	@Override
	public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
		return codec.encode(packet);
	}

	@Override
	public void handler(Packet packet, ChannelContext channelContext) throws Exception {
		// 偷懒使用的 EncodedPacket，直接处理的 byte 数据，大家可以自定义编解码
		String hexString = ByteBufferUtil.toHexString(((EncodedPacket) packet).getBytes());
		System.out.println(hexString);
	}
}
