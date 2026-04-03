package net.dreamlu.mica.net.core.clickhouse.core;

import net.dreamlu.mica.net.client.intf.TioClientListener;
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.Tio;
import net.dreamlu.mica.net.core.clickhouse.config.ClickHouseDefines;
import net.dreamlu.mica.net.core.clickhouse.message.AuthMessage;

public class ClickHouseClientListener implements TioClientListener {

	@Override
	public void onAfterConnected(ChannelContext context, boolean isConnected, boolean isReconnect) throws Exception {
		String clientName = "client";
		long clientReversion = ClickHouseDefines.CLIENT_REVISION;
		String defaultDatabase = "default";
		String clientUsername = "default";
		String clientPassword = "123456";
		AuthMessage packet = new AuthMessage(clientName, clientReversion, defaultDatabase, clientUsername, clientPassword);
		Tio.send(context, packet);
	}

}
