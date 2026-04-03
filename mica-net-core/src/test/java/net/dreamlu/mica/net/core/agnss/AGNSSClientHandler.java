package net.dreamlu.mica.net.core.agnss;

import net.dreamlu.mica.net.client.intf.TioClientHandler;
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.TioConfig;
import net.dreamlu.mica.net.core.exception.TioDecodeException;
import net.dreamlu.mica.net.core.intf.EncodedPacket;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.utils.buffer.ByteBufferUtil;

import java.nio.ByteBuffer;

public class AGNSSClientHandler implements TioClientHandler {
	private final AGNSSCodec codec;

	public AGNSSClientHandler(AGNSSCodec codec) {
		this.codec = codec;
	}

	@Override
	public Packet heartbeatPacket(ChannelContext context) {
		// 自定义心跳包
		return null;
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
		String hexString = ByteBufferUtil.toHexString(((EncodedPacket) packet).getBytes());
		System.out.println(hexString);
	}
}
