package org.tio.core.cluster.transport;

import org.tio.client.intf.TioClientListener;
import org.tio.core.ChannelContext;

/**
 * 集群客户端监听器
 *
 * @author L.cm
 */
public class ClusterTcpClientListener implements TioClientListener {

	@Override
	public void onAfterConnected(ChannelContext context, boolean isConnected, boolean isReconnect) throws Exception {
	}

	@Override
	public void onBeforeClose(ChannelContext context, Throwable throwable, String remark, boolean isRemove) throws Exception {
	}
}
