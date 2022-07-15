package org.tio.core.tcp;

import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.server.intf.TioServerHandler;
import org.tio.utils.buffer.ByteBufferUtil;

import java.nio.ByteBuffer;

public class TestTioServerHandler implements TioServerHandler {

	@Override
	public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext) throws TioDecodeException {
		String hexDump = ByteBufferUtil.hexDump(buffer);
		System.out.println(hexDump);
		return null;
	}

	@Override
	public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
		return null;
	}

	@Override
	public void handler(Packet packet, ChannelContext channelContext) throws Exception {

	}
}
