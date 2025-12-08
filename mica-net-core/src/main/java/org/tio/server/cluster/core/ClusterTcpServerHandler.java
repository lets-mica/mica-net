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

import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.core.TioConfig;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.server.cluster.codec.ClusterMessageDecoder;
import org.tio.server.cluster.codec.ClusterMessageEncoder;
import org.tio.server.cluster.message.*;
import org.tio.server.intf.TioServerHandler;

import java.nio.ByteBuffer;

/**
 * tcp 集群处理器
 *
 * @author L.cm
 */
public class ClusterTcpServerHandler implements TioServerHandler {
	private final ClusterImpl clusterApi;
	private final ClusterMessageEncoder messageEncoder;
	private final ClusterMessageDecoder messageDecoder;
	/**
	 * 消息监听器
	 */
	private final ClusterMessageListener messageListener;

	public ClusterTcpServerHandler(ClusterImpl clusterApi,
								   ClusterMessageDecoder messageDecoder,
								   ClusterMessageListener messageListener) {
		this.clusterApi = clusterApi;
		this.messageEncoder = ClusterMessageEncoder.INSTANCE;
		this.messageDecoder = messageDecoder;
		this.messageListener = messageListener;
	}

	@Override
	public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext context) throws TioDecodeException {
		return messageDecoder.decode(context, buffer, readableLength);
	}

	@Override
	public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext context) {
		return messageEncoder.encode((AbsClusterMessage) packet);
	}

	@Override
	public void handler(Packet packet, ChannelContext context) throws Exception {
		// 心跳 ping 消息
		if (packet instanceof ClusterPingMessage) {
			handlerPingMessage(context);
		} else if (packet instanceof ClusterSyncMessage) {
			handlerSyncMessage(context, (ClusterSyncMessage) packet);
		} else if (packet instanceof ClusterDataMessage) {
			handlerDataMessage((ClusterDataMessage) packet);
		} else if (packet instanceof ClusterJoinMessage) {
			handlerJoinMessage((ClusterJoinMessage) packet);
		}
	}

	/**
	 * 处理 ping 消息
	 *
	 * @param context ChannelContext
	 */
	private static void handlerPingMessage(ChannelContext context) {
		Tio.send(context, ClusterPongMessage.INSTANCE);
	}

	/**
	 * 处理同步数据消息
	 *
	 * @param context ChannelContext
	 * @param message ClusterSyncMessage
	 */
	private void handlerSyncMessage(ChannelContext context, ClusterSyncMessage message) {
		// 处理消息
		messageListener.onMessage(message);
		// 回复 ack
		Tio.send(context, message.toAckMessage());
	}

	/**
	 * 处理数据消息
	 *
	 * @param message ClusterDataMessage
	 */
	private void handlerDataMessage(ClusterDataMessage message) {
		// 处理消息
		messageListener.onMessage(message);
	}

	/**
	 * 处理新节点加入
	 *
	 * @param message ClusterJoinMessage
	 */
	private void handlerJoinMessage(ClusterJoinMessage message) {
		// 处理消息
		clusterApi.addJoinMember(message.getJoinMember());
	}

}
