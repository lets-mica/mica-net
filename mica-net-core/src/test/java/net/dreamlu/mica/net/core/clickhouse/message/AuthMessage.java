package net.dreamlu.mica.net.core.clickhouse.message;

/**
 * 认证包
 *
 * @author L.cm
 */
public class AuthMessage extends ClickHouseMessage {

	private final String clientName;
	private final long clientReversion;
	private final String defaultDatabase;
	private final String clientUsername;
	private final String clientPassword;

	public AuthMessage(String clientName, long clientReversion, String defaultDatabase, String clientUsername, String clientPassword) {
        super(RequestMessageType.AUTH);
        this.clientName = clientName;
		this.clientReversion = clientReversion;
		this.defaultDatabase = defaultDatabase;
		this.clientUsername = clientUsername;
		this.clientPassword = clientPassword;
	}

	public String getClientName() {
		return clientName;
	}

	public long getClientReversion() {
		return clientReversion;
	}

	public String getDefaultDatabase() {
		return defaultDatabase;
	}

	public String getClientUsername() {
		return clientUsername;
	}

	public String getClientPassword() {
		return clientPassword;
	}

	@Override
	public String toString() {
		return "AuthPacket{" +
			"clientName='" + clientName + '\'' +
			", clientReversion=" + clientReversion +
			", defaultDatabase='" + defaultDatabase + '\'' +
			", clientUsername='" + clientUsername + '\'' +
			", clientPassword='" + clientPassword + '\'' +
			'}';
	}
}
