package org.tio.core.clickhouse.core;

import org.tio.client.intf.TioClientHandler;
import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.clickhouse.config.ClickHouseDefines;
import org.tio.core.clickhouse.message.AuthMessage;
import org.tio.core.clickhouse.message.PingMessage;
import org.tio.core.clickhouse.message.ResponseMessageType;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.utils.buffer.ByteBufferUtil;
import org.tio.utils.hutool.FastByteBuffer;

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
