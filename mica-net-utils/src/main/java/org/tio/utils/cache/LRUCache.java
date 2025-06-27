package org.tio.utils.cache;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU (least recently used)最近最久未使用缓存<br>
 * 根据使用时间来判定对象是否被持续缓存<br>
 * 当对象被访问时放入缓存，当缓存满了，最久未被使用的对象将被移除。<br>
 * 此缓存基于LinkedHashMap，因此当被缓存的对象每被访问一次，这个对象的key就到链表头部。<br>
 * 这个算法简单并且非常快，他比FIFO有一个显著优势是经常使用的对象不太可能被移除缓存。<br>
 * 缺点是当缓存满时，不能被很快的访问。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @author Looly, jodd
 */
public class LRUCache<K extends Serializable, V extends Serializable> extends ReentrantCache<K, V> {
	private static final long serialVersionUID = 1L;

	/**
	 * 构造<br>
	 * 默认无超时
	 *
	 * @param capacity 容量
	 */
	public LRUCache(int capacity) {
		this(capacity, 0);
	}

	/**
	 * 构造<br>
	 * 默认无超时
	 *
	 * @param capacity 容量
	 * @param listener cache 监听器
	 */
	public LRUCache(int capacity, CacheListener<K, V> listener) {
		this(capacity, 0, listener);
	}

	/**
	 * 构造
	 *
	 * @param capacity 容量
	 * @param timeout  默认超时时间，单位：毫秒
	 */
	public LRUCache(int capacity, long timeout) {
		this(capacity, timeout, null);
	}

	/**
	 * 构造
	 *
	 * @param capacity 容量
	 * @param timeout  默认超时时间，单位：毫秒
	 * @param listener cache 监听器
	 */
	public LRUCache(int capacity, long timeout, CacheListener<K, V> listener) {
		super(getCacheMap(capacity, listener), capacity, timeout);
		super.setListener(listener);
	}

	private static <K extends Serializable, V extends Serializable> Map<K, CacheObj<K, V>> getCacheMap(
		int capacity, CacheListener<K, V> listener) {
		return new LinkedHashMap<K, CacheObj<K, V>>(capacity + 1, .75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<K, CacheObj<K, V>> eldest) {
				if (this.size() > capacity) {
					if (null != listener) {
						CacheObj<K, V> entry = eldest.getValue();
						listener.onRemove(entry.key, entry.obj);
					}
					return true;
				} else {
					return false;
				}
			}
		};
	}

	// ---------------------------------------------------------------- prune

	@Override
	public AbstractCache<K, V> setListener(CacheListener<K, V> listener) {
		throw new IllegalArgumentException("请使用构造方法添加 CacheListener 参数");
	}

	/**
	 * 只清理超时对象，LRU的实现会交给{@code LinkedHashMap}
	 */
	@Override
	protected int pruneCache() {
		if (!isPruneExpiredActive()) {
			return 0;
		}
		int count = 0;
		Iterator<CacheObj<K, V>> values = cacheObjIter();
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
}
