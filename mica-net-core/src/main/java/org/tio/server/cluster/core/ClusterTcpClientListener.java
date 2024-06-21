/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & www.dreamlu.net).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
