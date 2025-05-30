package org.tio.utils.cache;

import org.tio.utils.timer.DefaultTimerTaskService;
import org.tio.utils.timer.TimerTask;
import org.tio.utils.timer.TimerTaskService;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 定时缓存<br>
 * 此缓存没有容量限制，对象只有在过期后才会被移除
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @author Looly, L.cm
 */
public class TimedCache<K extends Serializable, V extends Serializable> extends ReentrantCache<K, V> {
	private static final long serialVersionUID = 1L;

	/**
	 * 时间轮
	 */
	private final transient TimerTaskService timerTaskService;
	/**
	 * 正在执行的定时任务
	 */
	private transient TimerTask timerTask;

	/**
	 * 构造
	 *
	 * @param ttl 超时（过期）时长，单位毫秒
	 */
	public TimedCache(long ttl) {
		this(ttl, (int) ttl, new HashMap<>());
	}

	/**
	 * 构造
	 *
	 * @param ttl       过期时长，单位为毫秒
	 * @param cleanupMs 清理周期，单位为毫秒
	 */
	public TimedCache(long ttl, int cleanupMs) {
		this(ttl, cleanupMs, new HashMap<>());
	}

	/**
	 * 构造
	 *
	 * @param ttl       过期时长，单位为毫秒
	 * @param cleanupMs 清理周期
	 * @param map       存储缓存对象的map
	 */
	public TimedCache(long ttl, int cleanupMs, Map<K, CacheObj<K, V>> map) {
		super(map, 0, ttl);
		this.timerTaskService = new DefaultTimerTaskService();
		this.timerTaskService.start();
		this.schedulePrune(cleanupMs);
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
