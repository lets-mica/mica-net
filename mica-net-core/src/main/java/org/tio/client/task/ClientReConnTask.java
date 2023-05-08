package org.tio.client.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.client.ClientChannelContext;
import org.tio.client.ReconnConf;
import org.tio.client.TioClient;
import org.tio.client.TioClientConfig;
import org.tio.core.ChannelContext;
import org.tio.core.ssl.SslFacadeContext;
import org.tio.utils.timer.Timer;
import org.tio.utils.timer.TimerTask;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 客户端重连任务
 *
 * @author L.cm
 */
public class ClientReConnTask extends TimerTask {
	private static final Logger logger = LoggerFactory.getLogger(ClientReConnTask.class);
	private final Timer timer;
	private final TioClient tioClient;
	private final ReconnConf reconnConf;

	public ClientReConnTask(Timer timer, TioClient tioClient, ReconnConf reconnConf) {
		super(reconnConf.getInterval());
		this.timer = timer;
		this.tioClient = tioClient;
		this.reconnConf = reconnConf;
	}

	private ClientReConnTask(long delayMs, Timer timer, TioClient tioClient, ReconnConf reconnConf) {
		super(delayMs);
		this.timer = timer;
		this.tioClient = tioClient;
		this.reconnConf = reconnConf;
	}

	private static ClientReConnTask from(long delayMs, ClientReConnTask task) {
		return new ClientReConnTask(delayMs, task.timer, task.tioClient, task.reconnConf);
	}

	@Override
	public void run() {
		// 已经停止，跳过本次
		TioClientConfig tioClientConfig = tioClient.getTioClientConfig();
		if (tioClientConfig.isStopped()) {
			// 添加 task，保持后续执行
			timer.add(this);
			return;
		}
		int connectionSize = tioClientConfig.connections.size();
		logger.info("connecteds:{}, closeds:{}, connections:{}", tioClientConfig.connecteds.size(), tioClientConfig.closeds.size(), connectionSize);
		LinkedBlockingQueue<ChannelContext> queue = reconnConf.getQueue();
		ClientChannelContext channelContext = null;
		try {
			channelContext = (ClientChannelContext) queue.take();
		} catch (InterruptedException e1) {
			Thread.currentThread().interrupt();
			logger.error(e1.getMessage(), e1);
		}
		// 未连接的和已经删除的，不需要重新再连
		if (channelContext == null || channelContext.isRemoved) {
			// 添加 task，保持后续执行
			timer.add(this);
			return;
		}
		SslFacadeContext sslFacadeContext = channelContext.sslFacadeContext;
		if (sslFacadeContext != null) {
			sslFacadeContext.setHandshakeCompleted(false);
		}
		long sleepTime = reconnConf.getInterval() - (System.currentTimeMillis() - channelContext.stat.timeInReconnQueue);
		if (sleepTime > 0) {
			// 添加 task，保持后续执行
			timer.add(ClientReConnTask.from(sleepTime, this));
		} else {
			// 添加 task，保持后续执行
			timer.add(this);
		}
		// 已经删除的和已经连上的，不需要重新再连
		if (channelContext.isRemoved || !channelContext.isClosed) {
			return;
		}
		channelContext.getReconnCount().incrementAndGet();
		ReentrantReadWriteLock closeLock = channelContext.closeLock;
		ReentrantReadWriteLock.WriteLock writeLock = closeLock.writeLock();
		writeLock.lock();
		try {
			// 已经连上了，不需要再重连了
			if (!channelContext.isClosed) {
				return;
			}
			long start = System.currentTimeMillis();
			tioClient.reconnect(channelContext, 2);
			long end = System.currentTimeMillis();
			long iv = end - start;
			logger.error("{}, 第{}次重连,重连耗时:{} ms", channelContext, channelContext.getReconnCount(), iv);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			writeLock.unlock();
		}
	}

}
