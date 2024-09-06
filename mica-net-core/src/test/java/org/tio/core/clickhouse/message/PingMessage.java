package org.tio.core.clickhouse.message;

public class PingMessage extends ClickHouseMessage {
	public static final PingMessage INSTANCE = new PingMessage();

	public PingMessage() {
		super(RequestMessageType.PING);
	}
}
