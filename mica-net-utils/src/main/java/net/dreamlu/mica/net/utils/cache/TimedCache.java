package net.dreamlu.mica.net.utils.cache;

import net.dreamlu.mica.net.utils.timer.DefaultTimerTaskService;
import net.dreamlu.mica.net.utils.timer.TimerTask;
import net.dreamlu.mica.net.utils.timer.TimerTaskService;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 定时缓存<br>
 * 此缓存没有容量限制，对象只有在过期后才会被移除
 * <p>
 * 过期判定在读路径上即时完成（{@code get} 命中即校验），定时清理只负责回收
 * 「无人访问的过期条目」，因此清理周期按秒级即可，不支持毫秒级精度。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @author Looly, L.cm
 */
public class TimedCache<K extends Serializable, V extends Serializable> extends ReentrantCache<K, V> implements Closeable {
	private static final long serialVersionUID = 1L;

	/**
	 * 清理周期下限，单位毫秒。
	 * <p>
	 * 清理任务由时间轮调度，而时间轮的最小粒度是 {@link DefaultTimerTaskService#DEFAULT_TICK_MS}。
	 * 周期小于一个 tick 时，{@code TimingWheel#add} 会把任务当成「已到期」而立即执行（延时退化为 0），
	 * 而任务执行完又会重新入队，于是退化成空转，持续占用一个 CPU 核，并把同进程的线程调度拖慢。
	 * <p>
	 * 由于过期判定本来就在读路径上即时完成，这里把清理周期兜底到秒级即可。
	 */
	public static final long MIN_CLEANUP_MS = 1000L;

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
	 * <p>
	 * 清理周期兜底到 {@link #MIN_CLEANUP_MS}（秒级），避免小于时间轮粒度时任务立即执行导致空转。
	 *
	 * @param delay 间隔时长，单位毫秒
	 */
	protected void schedulePrune(long delay) {
		long period = Math.max(delay, MIN_CLEANUP_MS);
		this.timerTask = timerTaskService.addTask(timer -> new TimerTask(period) {
			@Override
			public void run() {
				timer.add(this);
				// 必须走 prune()（内部持锁），直接调用 pruneCache() 会与业务线程并发读写底层 map
				TimedCache.this.prune();
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
