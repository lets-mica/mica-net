package net.dreamlu.mica.net.core.clickhouse.message;

import net.dreamlu.mica.net.core.intf.Packet;

public class ClickHouseMessage extends Packet {
	private final RequestMessageType messageType;

	public ClickHouseMessage(RequestMessageType messageType) {
		this.messageType = messageType;
	}

	public RequestMessageType getMessageType() {
		return messageType;
	}
}
