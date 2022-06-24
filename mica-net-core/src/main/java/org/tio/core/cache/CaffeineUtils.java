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

package org.tio.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.tio.utils.hutool.CollUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * caffeine 工具类
 *
 * @author L.cm
 */
public class CaffeineUtils {
	private static final ConcurrentMap<String, com.github.benmanes.caffeine.cache.Cache<Object, Object>> CACHE_MANAGER = new ConcurrentHashMap<>();

	/**
	 * 获取 caffeine cache
	 *
	 * @param cacheName         cacheName
	 * @param timeToLiveSeconds timeToLiveSeconds
	 * @param entryCapacity     entryCapacity
	 * @param <K>               key 泛型
	 * @param <V>               value 泛型
	 * @return Cache
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> Cache<K, V> getCache(String cacheName, long timeToLiveSeconds, long entryCapacity) {
		return (Cache<K, V>) CollUtil.computeIfAbsent(CACHE_MANAGER, cacheName, key -> Caffeine.newBuilder()
			.maximumSize(entryCapacity)
			.expireAfterWrite(timeToLiveSeconds, TimeUnit.SECONDS)
			.build());
	}

	/**
	 * 构建 caffeine cache
	 *
	 * @param timeToLiveSeconds timeToLiveSeconds
	 * @param entryCapacity     entryCapacity
	 * @param removalListener   IpStatRemovalListener
	 * @return Cache
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> Cache<K, V> getIpStatCache(String cacheName, long timeToLiveSeconds, long entryCapacity, IpStatRemovalListener removalListener) {
		return (Cache<K, V>) CollUtil.computeIfAbsent(CACHE_MANAGER, cacheName, key -> Caffeine.newBuilder()
			.maximumSize(entryCapacity)
			.expireAfterWrite(timeToLiveSeconds, TimeUnit.SECONDS)
			.removalListener(removalListener)
			.build());
	}

}
