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

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.CacheManager;

import java.util.concurrent.TimeUnit;

/**
 * cache2k 工具类
 *
 * @author L.cm
 */
public class Cache2kUtils {

	/**
	 * 获取 Cache2k cache
	 *
	 * @param cacheName         cacheName
	 * @param timeToLiveSeconds timeToLiveSeconds
	 * @param entryCapacity     entryCapacity
	 * @param keyType           keyType
	 * @param valueType         valueType
	 * @param <K>               key 泛型
	 * @param <V>               value 泛型
	 * @return Cache
	 */
	public static <K, V> Cache<K, V> getCache(String cacheName, long timeToLiveSeconds, long entryCapacity, Class<K> keyType, Class<V> valueType) {
		CacheManager cacheManager = CacheManager.getInstance();
		Cache<K, V> cache = cacheManager.getCache(cacheName);
		if (cache == null) {
			synchronized (Cache.class) {
				cache = cacheManager.getCache(cacheName);
				if (cache == null) {
					return Cache2kBuilder.of(keyType, valueType)
						.manager(cacheManager)
						.name(cacheName)
						.expireAfterWrite(timeToLiveSeconds, TimeUnit.SECONDS)
						.entryCapacity(entryCapacity)
						.build();
				}
			}
		}
		return cache;
	}

}
