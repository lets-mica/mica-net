package org.tio.utils.cache;

import org.tio.utils.timer.DefaultTimerTaskService;
import org.tio.utils.timer.TimerTask;
import org.tio.utils.timer.TimerTaskService;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 定时缓存<br>
 * 此缓存没有容量限制，对象只有在过期后才会被移除
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @author Looly, L.cm
 */
public class TimedCache<K extends Serializable, V extends Serializable> extends ReentrantCache<K, V> implements Closeable {
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
		this(ttl, new HashMap<>());
	}

	/**
	 * 构造
	 *
	 * @param ttl 过期时长，单位为毫秒
	 * @param map 存储缓存对象的map
	 */
	public TimedCache(long ttl, Map<K, CacheObj<K, V>> map) {
		this(ttl, ttl, map);
	}

	/**
	 * 构造
	 *
	 * @param ttl       过期时长，单位为毫秒
	 * @param cleanupMs 清理周期，单位为毫秒
	 */
	public TimedCache(long ttl, long cleanupMs) {
		this(ttl, cleanupMs, new HashMap<>());
	}

	/**
	 * 构造
	 *
	 * @param ttl       过期时长，单位为毫秒
	 * @param cleanupMs 清理周期
	 * @param map       存储缓存对象的map
	 */
	public TimedCache(long ttl, long cleanupMs, Map<K, CacheObj<K, V>> map) {
		super(map, 0, ttl);
		this.timerTaskService = new DefaultTimerTaskService();
		this.timerTaskService.start();
		this.schedulePrune(cleanupMs);
	}

	/**
	 * 从缓存中获得对象，当对象不在缓存中或已经过期返回{@code null}
	 * <p>
	 * 调用此方法时，会检查上次调用时间，如果与当前时间差值大于超时时间返回{@code null}，否则返回值。
	 * <p>
	 * @param key 键
	 * @return 键对应的对象
	 */
	@Override
	public V get(K key) {
		return super.get(key, false);
	}

	/**
	 * 从缓存中获得对象，当对象不在缓存中或已经过期返回 Supplier 回调产生的对象
	 * <p>
	 * 调用此方法时，会检查上次调用时间，如果与当前时间差值大于超时时间返回{@code null}，否则返回值。
	 * <p>
	 *
	 * @param key      键
	 * @param supplier 如果不存在回调方法，用于生产值对象
	 * @return 值对象
	 */
	@Override
	public V get(K key, Supplier<V> supplier) {
		return super.get(key, false, supplier);
	}

	/**
	 * 获取并刷新 ttl，每次调用此方法会刷新最后访问时间，也就是说会重新计算超时时间。
	 *
	 * @param key key
	 * @return V 值
	 */
	public V getAndRefresh(K key) {
		return super.get(key, true);
	}

	/**
	 * 获取并刷新 ttl，每次调用此方法会刷新最后访问时间，也就是说会重新计算超时时间。
	 *
	 * @param key      key
	 * @param supplier supplier
	 * @return 值
	 */
	public V getAndRefresh(K key, Supplier<V> supplier) {
		return super.get(key, true, supplier);
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

	@Override
	public void close() throws IOException {
		// 取消定时任务
		this.cancelPruneSchedule();
		// 停止定时任务
		this.timerTaskService.stop();
	}

}
