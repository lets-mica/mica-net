package org.tio.core.cluster.transport;

import org.tio.core.ChannelContext;
import org.tio.server.intf.TioServerListener;

/**
 * 集群服务监听器
 *
 * @author L.cm
 */
public class ClusterTcpServerListener implements TioServerListener {

	@Override
	public boolean onHeartbeatTimeout(ChannelContext channelContext, Long interval, int heartbeatTimeoutCount) {
		return false;
	}

	@Override
	public void onAfterConnected(ChannelContext channelContext, boolean isConnected, boolean isReconnect) throws Exception {
	}

	@Override
	public void onBeforeClose(ChannelContext channelContext, Throwable throwable, String remark, boolean isRemove) throws Exception {
	}
}
