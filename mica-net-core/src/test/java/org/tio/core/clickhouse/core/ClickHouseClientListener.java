package org.tio.core.clickhouse.core;

import org.tio.client.intf.TioClientListener;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.core.clickhouse.config.ClickHouseDefines;
import org.tio.core.clickhouse.message.AuthMessage;

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
