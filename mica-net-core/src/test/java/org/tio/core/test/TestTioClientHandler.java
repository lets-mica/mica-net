package org.tio.core.test;

import org.tio.client.intf.TioClientHandler;
import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.EncodedPacket;
import org.tio.core.intf.Packet;
import org.tio.utils.SysConst;
import org.tio.utils.buffer.ByteBufferUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 测试
 */
public class TestTioClientHandler implements TioClientHandler {
	@Override
	public Packet heartbeatPacket(ChannelContext channelContext) {
		ByteBuffer buffer = ByteBuffer.allocate(11);
		buffer.put("AdTest255".getBytes(StandardCharsets.UTF_8));
		buffer.put(SysConst.CR);
		buffer.put(SysConst.LF);
		return new EncodedPacket(buffer.array());
	}

	@Override
	public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext) throws TioDecodeException {
		String dump = ByteBufferUtil.hexDump(buffer);
		System.out.println(dump);
		return new EncodedPacket(buffer.array());
	}

	@Override
	public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
		return ByteBuffer.wrap(((EncodedPacket) packet).getBytes());
	}

	@Override
	public void handler(Packet packet, ChannelContext channelContext) throws Exception {

	}
}
