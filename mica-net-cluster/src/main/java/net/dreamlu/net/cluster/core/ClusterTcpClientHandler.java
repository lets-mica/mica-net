package net.dreamlu.net.cluster.core;

import org.tio.client.intf.TioClientHandler;
import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import net.dreamlu.net.cluster.codec.ClusterMessageDecoder;
import net.dreamlu.net.cluster.codec.ClusterMessageEncoder;
import net.dreamlu.net.cluster.message.AbsClusterMessage;
import net.dreamlu.net.cluster.message.ClusterPingMessage;
import net.dreamlu.net.cluster.message.ClusterSyncAckMessage;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;

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
	private final ConcurrentMap<String, CompletableFuture<ClusterSyncAckMessage>> syncMessageMap;

	public ClusterTcpClientHandler(ClusterMessageDecoder messageDecoder,
								   ConcurrentMap<String, CompletableFuture<ClusterSyncAckMessage>> syncMessageMap) {
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
		return messageEncoder.encode(context, (AbsClusterMessage) packet);
	}

	@Override
	public void handler(Packet packet, ChannelContext context) throws Exception {
		if (packet instanceof ClusterSyncAckMessage) {
			ClusterSyncAckMessage message = (ClusterSyncAckMessage) packet;
			String messageId = message.getMessageId();
			CompletableFuture<ClusterSyncAckMessage> future = syncMessageMap.get(messageId);
			if (future != null) {
				future.complete(message);
			}
		}
	}
}
