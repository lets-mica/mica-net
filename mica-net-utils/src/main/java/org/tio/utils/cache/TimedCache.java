package org.tio.utils.cache;

import org.tio.utils.timer.DefaultTimerTaskService;
import org.tio.utils.timer.TimerTask;
import org.tio.utils.timer.TimerTaskService;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 定时缓存<br>
 * 此缓存没有容量限制，对象只有在过期后才会被移除
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @author Looly
 */
public class TimedCache<K, V> extends StampedCache<K, V> {
	private static final long serialVersionUID = 1L;

	/**
	 * 时间轮
	 */
	private final TimerTaskService timerTaskService;
	/**
	 * 正在执行的定时任务
	 */
	private TimerTask timerTask;

	/**
	 * 构造
	 *
	 * @param timeout 超时（过期）时长，单位毫秒
	 */
	public TimedCache(long timeout) {
		this(timeout, new HashMap<>());
	}

	/**
	 * 构造
	 *
	 * @param timeout 过期时长
	 * @param map     存储缓存对象的map
	 */
	public TimedCache(long timeout, Map<K, CacheObj<K, V>> map) {
		this.capacity = 0;
		this.timeout = timeout;
		this.cacheMap = map;
		this.timerTaskService = new DefaultTimerTaskService();
		this.timerTaskService.start();
		this.schedulePrune(timeout);
	}

	// ---------------------------------------------------------------- prune

	/**
	 * 清理过期对象
	 *
	 * @return 清理数
	 */
	@Override
	protected int pruneCache() {
		int count = 0;
		final Iterator<CacheObj<K, V>> values = cacheObjIter();
		CacheObj<K, V> co;
		while (values.hasNext()) {
			co = values.next();
			if (co.isExpired()) {
				values.remove();
				onRemove(co.key, co.obj);
				count++;
			}
		}
		return count;
	}

	// ---------------------------------------------------------------- auto prune

	/**
	 * 定时清理
	 *
	 * @param delay 间隔时长，单位毫秒
	 */
	protected void schedulePrune(long delay) {
		this.timerTask = timerTaskService.addTask(timer -> new TimerTask(delay) {
			@Override
			public void run() {
				timer.add(this);
				TimedCache.this.pruneCache();
			}
		});
	}

	/**
	 * 取消定时清理
	 */
	public void cancelPruneSchedule() {
		if (null != this.timerTask) {
			this.timerTask.cancel();
		}
	}

}
