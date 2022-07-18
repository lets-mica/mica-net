package org.tio.core.tcp;

import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.core.tcp.modbus.ModbusMessage;
import org.tio.server.intf.TioServerHandler;
import org.tio.utils.buffer.ByteBufferUtil;

import java.nio.ByteBuffer;

public class TestTioServerHandler implements TioServerHandler {
	/**
	 * Constant <code>MAX_FUNCTION_CODE=(byte) 0x80</code>
	 */
	protected static final byte MAX_FUNCTION_CODE = (byte) 0x80;

	@Override
	public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext) throws TioDecodeException {
		short slaveId = ByteBufferUtil.readUnsignedByte(buffer);
		byte functionCode = buffer.get();
		boolean isException = false;
		if (greaterThan(functionCode, MAX_FUNCTION_CODE)) {
			isException = true;
			functionCode -= MAX_FUNCTION_CODE;
		}
		String hexDump = ByteBufferUtil.hexDump(buffer);
		System.out.println(hexDump);
		return new ModbusMessage(slaveId, functionCode, isException);
	}

	private static boolean greaterThan(byte b1, byte b2) {
		int i1 = b1 & 0xff;
		int i2 = b2 & 0xff;
		return i1 > i2;
	}

	@Override
	public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
		return null;
	}

	@Override
	public void handler(Packet packet, ChannelContext channelContext) throws Exception {

	}
}
