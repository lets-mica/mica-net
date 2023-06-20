package org.tio.server.cluster.core;

import org.tio.server.cluster.message.ClusterJoinMessage;
import org.tio.client.intf.TioClientListener;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;

/**
 * 集群客户端监听器
 *
 * @author L.cm
 */
public class ClusterTcpClientListener implements TioClientListener {
	private final ClusterApi clusterApi;

	public ClusterTcpClientListener(ClusterApi clusterApi) {
		this.clusterApi = clusterApi;
	}

	@Override
	public void onAfterConnected(ChannelContext context, boolean isConnected, boolean isReconnect) throws Exception {
		// 1. 如果自己是后加入的成员，连接成功之后发送一条加入的消息
		boolean isLateJoinMember = clusterApi.isLateJoinMember();
		if (isConnected && !isReconnect && isLateJoinMember) {
			Tio.send(context, new ClusterJoinMessage(clusterApi.getLocalMember()));
		}
	}

	@Override
	public void onBeforeClose(ChannelContext context, Throwable throwable, String remark, boolean isRemove) throws Exception {
		if (throwable != null) {
			throwable.printStackTrace();
		}
	}

}
