package org.tio.core.tcp;

import org.tio.client.intf.TioClientHandler;
import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.EncodedPacket;
import org.tio.core.intf.Packet;
import org.tio.utils.buffer.ByteBufferUtil;

import java.nio.ByteBuffer;

public class TestTioClientHandler implements TioClientHandler {
	@Override
	public Packet heartbeatPacket(ChannelContext channelContext) {
		return null;
	}

	@Override
	public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext) throws TioDecodeException {
		return FixedLengthCodec.INSTANCE.decode(buffer, readableLength);
	}

	@Override
	public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
		return FixedLengthCodec.INSTANCE.encode(packet);
	}

	@Override
	public void handler(Packet packet, ChannelContext channelContext) throws Exception {
		String hexString = ByteBufferUtil.toHexString(((EncodedPacket) packet).getBytes());
		System.out.println(hexString);
	}
}
