package net.dreamlu.mica.net.client.task;

import net.dreamlu.mica.net.client.ClientChannelContext;
import net.dreamlu.mica.net.client.ReconnConf;
import net.dreamlu.mica.net.client.TioClient;
import net.dreamlu.mica.net.client.TioClientConfig;
import net.dreamlu.mica.net.core.ssl.SslFacadeContext;
import net.dreamlu.mica.net.utils.timer.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 客户端重连任务
 *
 * @author L.cm
 */
public class ClientReConnTask extends TimerTask {
	private static final Logger logger = LoggerFactory.getLogger(ClientReConnTask.class);
	private final TioClient tioClient;
	private final ClientChannelContext channelContext;

	public ClientReConnTask(ClientChannelContext clientChannelContext, ReconnConf reconnConf) {
		super(reconnConf.getInterval());
		this.tioClient = reconnConf.getTioClient();
		this.channelContext = clientChannelContext;
	}

	@Override
	public void run() {
		if (channelContext == null) {
			return;
		}
		TioClientConfig tioClientConfig = (TioClientConfig) channelContext.getTioConfig();
		// 已经停止，跳过
		if (tioClientConfig.isStopped()) {
			return;
		}
		if (tioClientConfig.debug && logger.isInfoEnabled()) {
			logger.info("connecteds:{}, closeds:{}, connections:{}", tioClientConfig.connecteds.size(), tioClientConfig.closeds.size(), tioClientConfig.connections.size());
		}
		// 未连接的和已经删除的，不需要重新再连
		if (channelContext.isRemoved()) {
			return;
		}
		SslFacadeContext sslFacadeContext = channelContext.sslFacadeContext;
		if (sslFacadeContext != null) {
			sslFacadeContext.setHandshakeCompleted(false);
		}
		// 已经删除的和已经连上的，不需要重新再连
		if (channelContext.isRemoved() || !channelContext.isClosed()) {
			return;
		}
		int reConnCount = channelContext.getReConnCount().incrementAndGet();
		ReentrantReadWriteLock closeLock = channelContext.closeLock;
		ReentrantReadWriteLock.WriteLock writeLock = closeLock.writeLock();
		writeLock.lock();
		try {
			// 已经连上了，不需要再重连了
			if (!channelContext.isClosed()) {
				return;
			}
			long start = System.currentTimeMillis();
			tioClient.reconnect(channelContext, 2);
			long end = System.currentTimeMillis();
			long iv = end - start;
			logger.error("{}, 第{}次重连,重连耗时:{} ms", channelContext, reConnCount, iv);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			writeLock.unlock();
		}
	}

}
