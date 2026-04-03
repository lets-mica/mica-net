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

package net.dreamlu.mica.net.server.cluster.core;

import net.dreamlu.mica.net.client.intf.TioClientListener;
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.Node;
import net.dreamlu.mica.net.core.Tio;
import net.dreamlu.mica.net.server.cluster.message.ClusterJoinMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 集群客户端监听器
 *
 * @author L.cm
 */
public class ClusterTcpClientListener implements TioClientListener {
	private static final Logger log = LoggerFactory.getLogger(ClusterTcpClientListener.class);
	private final ClusterImpl clusterApi;

	public ClusterTcpClientListener(ClusterImpl clusterApi) {
		this.clusterApi = clusterApi;
	}

	@Override
	public void onAfterConnected(ChannelContext context, boolean isConnected, boolean isReconnect) throws Exception {
		if (isConnected) {
			// 1. 绑定映射关系
			Node serverNode = context.getServerNode();
			clusterApi.putMemberChannel(serverNode, context);
			// 2. 如果自己是后加入的成员，连接成功之后发送一条加入的消息
			// 只有后加入的成员才发 JOIN（种子成员启动时没有 JOIN 概念）
			// JOIN 的作用是通知对方"请主动连回我"，实现双向通信
			boolean isLateJoinMember = clusterApi.isLateJoinMember();
			if (!isReconnect && isLateJoinMember) {
				Tio.send(context, new ClusterJoinMessage(clusterApi.getLocalMember()));
			}
		}
	}

	@Override
	public void onBeforeClose(ChannelContext context, Throwable throwable, String remark, boolean isRemove) throws Exception {
		log.error("集群链接断开 context:{} remark:{} isRemove:{}", context, remark, isRemove, throwable);
		Node serverNode = context.getServerNode();
		clusterApi.removeMemberChannel(serverNode, context);
	}

}
