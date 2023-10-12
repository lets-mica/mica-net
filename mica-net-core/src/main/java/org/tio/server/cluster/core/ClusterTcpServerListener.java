package org.tio.server.cluster.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.ChannelContext;
import org.tio.server.intf.TioServerListener;

/**
 * 集群服务监听器
 *
 * @author L.cm
 */
public class ClusterTcpServerListener implements TioServerListener {
	private static final Logger log = LoggerFactory.getLogger(ClusterTcpServerListener.class);

	@Override
	public boolean onHeartbeatTimeout(ChannelContext context, long interval, int heartbeatTimeoutCount) {
		return false;
	}

	@Override
	public void onAfterConnected(ChannelContext context, boolean isConnected, boolean isReconnect) throws Exception {
	}

	@Override
	public void onBeforeClose(ChannelContext context, Throwable throwable, String remark, boolean isRemove) throws Exception {
		if (throwable != null) {
			log.error(throwable.getMessage(), throwable);
		}
	}
}
