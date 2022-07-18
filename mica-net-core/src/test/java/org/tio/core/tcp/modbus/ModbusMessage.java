package org.tio.core.tcp.modbus;

import org.tio.core.intf.Packet;

public class ModbusMessage extends Packet {

	private short slaveId;
	private byte functionCode;
	private boolean isException;

	public ModbusMessage() {
	}

	public ModbusMessage(short slaveId, byte functionCode, boolean isException) {
		this.slaveId = slaveId;
		this.functionCode = functionCode;
		this.isException = isException;
	}

	public short getSlaveId() {
		return slaveId;
	}

	public void setSlaveId(short slaveId) {
		this.slaveId = slaveId;
	}

	public byte getFunctionCode() {
		return functionCode;
	}

	public void setFunctionCode(byte functionCode) {
		this.functionCode = functionCode;
	}

	public boolean isException() {
		return isException;
	}

	public void setException(boolean exception) {
		isException = exception;
	}
}
