package org.tio.core.clickhouse.message;

import org.tio.core.intf.Packet;

public class ClickHouseMessage extends Packet {
	private final RequestMessageType messageType;

	public ClickHouseMessage(RequestMessageType messageType) {
		this.messageType = messageType;
	}

	public RequestMessageType getMessageType() {
		return messageType;
	}
}
