package net.dreamlu.mica.net.core.clickhouse.message;

public enum ResponseMessageType {
	AUTH(0),
	DATA(1),
	EXCEPTION(2),
	PROGRESS(3),
	PONG(4),
	END_OF_STREAM(5),
	PROFILE_INFO(6),
	TOTALS(7),
	EXTREMES(8),
	TABLES_STATUS_RESPONSE(9);

	private final int id;

	ResponseMessageType(int id) {
		this.id = id;
	}

	public long id() {
		return id;
	}

	public static ResponseMessageType get(int id) {
		switch (id) {
			case 0: return AUTH;
			case 1: return DATA;
		}
		return null;
	}

}
