package org.tio.core.clickhouse.message;

public enum RequestMessageType {
	AUTH(0),
	QUERY(1),
	DATA(2),
	PING(4);

	private final int id;

	RequestMessageType(int id) {
		this.id = id;
	}

	public long id() {
		return id;
	}
}
