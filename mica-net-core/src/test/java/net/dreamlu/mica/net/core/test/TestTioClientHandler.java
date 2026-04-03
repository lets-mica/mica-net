package net.dreamlu.mica.net.core.test;

import net.dreamlu.mica.net.client.intf.TioClientHandler;
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.TioConfig;
import net.dreamlu.mica.net.core.exception.TioDecodeException;
import net.dreamlu.mica.net.core.intf.EncodedPacket;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.utils.SysConst;
import net.dreamlu.mica.net.utils.buffer.ByteBufferUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 测试
 */
public class TestTioClientHandler implements TioClientHandler {
	@Override
	public Packet heartbeatPacket(ChannelContext context) {
		ByteBuffer buffer = ByteBuffer.allocate(11);
		buffer.put("AdTest255".getBytes(StandardCharsets.UTF_8));
		buffer.put(SysConst.CR);
		buffer.put(SysConst.LF);
		return new EncodedPacket(buffer.array());
	}

	@Override
	public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext) throws TioDecodeException {
		System.out.println("---------------接收-----------------");
		System.out.println(ByteBufferUtil.hexDump(buffer));
		return new EncodedPacket(buffer.array());
	}

	@Override
	public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
		ByteBuffer buffer = ByteBuffer.wrap(((EncodedPacket) packet).getBytes());
		System.out.println("---------------发送-----------------");
		System.out.println(ByteBufferUtil.hexDump(buffer));
		return buffer;
	}

	@Override
	public void handler(Packet packet, ChannelContext channelContext) throws Exception {

	}
}
