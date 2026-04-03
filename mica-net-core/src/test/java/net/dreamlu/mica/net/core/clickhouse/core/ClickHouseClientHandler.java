package net.dreamlu.mica.net.core.clickhouse.core;

import net.dreamlu.mica.net.client.intf.TioClientHandler;
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.TioConfig;
import net.dreamlu.mica.net.core.clickhouse.config.ClickHouseDefines;
import net.dreamlu.mica.net.core.clickhouse.message.AuthMessage;
import net.dreamlu.mica.net.core.clickhouse.message.PingMessage;
import net.dreamlu.mica.net.core.clickhouse.message.ResponseMessageType;
import net.dreamlu.mica.net.core.exception.TioDecodeException;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.utils.buffer.ByteBufferUtil;
import net.dreamlu.mica.net.utils.hutool.FastByteBuffer;

import java.nio.ByteBuffer;

public class ClickHouseClientHandler implements TioClientHandler  {
	@Override
	public Packet heartbeatPacket(ChannelContext context) {
		return PingMessage.INSTANCE;
	}

	@Override
	public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext context) throws TioDecodeException {
		if (readableLength > 0) {
			String hexDump = ByteBufferUtil.hexDump(buffer);
			System.out.println(hexDump);
			short type = ByteBufferUtil.readUnsignedByte(buffer);
			ResponseMessageType messageType = ResponseMessageType.get(type);
			System.out.println(messageType);
		}
		return null;
	}

	@Override
	public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext context) {
		if (packet instanceof AuthMessage) {
			AuthMessage message = (AuthMessage) packet;
			FastByteBuffer buffer = new FastByteBuffer();
			buffer.writeByte((byte) message.getMessageType().id());
			String s = ClickHouseDefines.NAME + " " + message.getClientName();
			buffer.writeVarLengthInt(s.length());
			buffer.writeString(s);
			buffer.writeVarLengthInt(ClickHouseDefines.MAJOR_VERSION);
			buffer.writeVarLengthInt(ClickHouseDefines.MINOR_VERSION);
			buffer.writeVarLengthInt((int) message.getClientReversion());
			String defaultDatabase = message.getDefaultDatabase();
			buffer.writeVarLengthInt(defaultDatabase.length());
			buffer.writeString(defaultDatabase);
			String clientUsername = message.getClientUsername();
			buffer.writeVarLengthInt(clientUsername.length());
			buffer.writeString(clientUsername);
			String clientPassword = message.getClientPassword();
			buffer.writeVarLengthInt(clientPassword.length());
			buffer.writeString(clientPassword);
			return buffer.toBuffer();
		}
		return null;
	}

	@Override
	public void handler(Packet packet, ChannelContext context) throws Exception {

	}
}
