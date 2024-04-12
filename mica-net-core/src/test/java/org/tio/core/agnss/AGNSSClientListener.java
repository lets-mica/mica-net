package org.tio.core.agnss;

import org.tio.client.intf.TioClientListener;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.core.intf.EncodedPacket;

import java.nio.charset.StandardCharsets;

public class AGNSSClientListener implements TioClientListener {

	@Override
	public void onAfterConnected(ChannelContext context, boolean isConnected, boolean isReconnect) throws Exception {
		// 连接成功之后发送认证
		String authText = "user=freetrial;pwd=123456;gnss=gps,bds;cmd=full;lat=39.1;lon=117.06;alt=0;";
		byte[] data = authText.getBytes(StandardCharsets.UTF_8);
		Tio.send(context, new EncodedPacket(data));
	}

}
