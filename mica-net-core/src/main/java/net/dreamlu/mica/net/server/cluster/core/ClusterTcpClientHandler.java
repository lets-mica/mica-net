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

import net.dreamlu.mica.net.client.intf.TioClientHandler;
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.TioConfig;
import net.dreamlu.mica.net.core.exception.TioDecodeException;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.server.cluster.codec.ClusterMessageDecoder;
import net.dreamlu.mica.net.server.cluster.codec.ClusterMessageEncoder;
import net.dreamlu.mica.net.server.cluster.message.AbsClusterMessage;
import net.dreamlu.mica.net.server.cluster.message.ClusterPingMessage;
import net.dreamlu.mica.net.server.cluster.message.ClusterSyncAckMessage;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;

/**
 * 集群客户端处理器
 *
 * @author L.cm
 */
public class ClusterTcpClientHandler implements TioClientHandler {
	private final ClusterMessageEncoder messageEncoder;
	private final ClusterMessageDecoder messageDecoder;
	/**
	 * 同步消息处理，key：messageId，value：CompletableFuture
	 */
	private final ConcurrentMap<Long, CompletableFuture<ClusterSyncAckMessage>> syncMessageMap;

	public ClusterTcpClientHandler(ClusterMessageDecoder messageDecoder,
	                               ConcurrentMap<Long, CompletableFuture<ClusterSyncAckMessage>> syncMessageMap) {
		this.messageEncoder = ClusterMessageEncoder.INSTANCE;
		this.messageDecoder = messageDecoder;
		this.syncMessageMap = syncMessageMap;
	}

	@Override
	public Packet heartbeatPacket(ChannelContext context) {
		return ClusterPingMessage.INSTANCE;
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
		if (packet instanceof ClusterSyncAckMessage) {
			ClusterSyncAckMessage message = (ClusterSyncAckMessage) packet;
			long messageId = message.getMessageId();
			CompletableFuture<ClusterSyncAckMessage> future = syncMessageMap.get(messageId);
			if (future != null) {
				future.complete(message);
			}
		}
	}
}
