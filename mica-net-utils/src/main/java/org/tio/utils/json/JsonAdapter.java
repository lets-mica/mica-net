/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & dreamlu.net).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tio.utils.json;

import org.tio.utils.mica.TypeUtils;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * json 适配器
 *
 * @author L.cm
 */
public interface JsonAdapter {

	/**
	 * 判断字符串是否是 json
	 *
	 * @param json json
	 * @return 是否是 json
	 */
	boolean isValidJson(String json);

	/**
	 * 转换成 json 字符串
	 *
	 * @param object object
	 * @return json 字符串
	 */
	String toJsonString(Object object);

	/**
	 * 转换成 json byte array
	 *
	 * @param object object
	 * @return json 字符串
	 */
	default byte[] toJsonBytes(Object object) {
		String jsonString = toJsonString(object);
		return jsonString == null ? null : jsonString.getBytes();
	}

	/**
	 * 将 json 字符串转成对象
	 *
	 * @param json  json
	 * @param clazz Class
	 * @param <T>   泛型
	 * @return 对象
	 */
	<T> T readValue(String json, Class<T> clazz);

	/**
	 * 将 json 字符串转成对象
	 *
	 * @param json json
	 * @param type Type
	 * @param <T>  泛型
	 * @return 对象
	 */
	<T> T readValue(String json, Type type);

	/**
	 * 将 json byte array 转成对象
	 *
	 * @param json  json
	 * @param clazz Class
	 * @param <T>   泛型
	 * @return 对象
	 */
	default <T> T readValue(byte[] json, Class<T> clazz) {
		return readValue(new String(json), clazz);
	}

	/**
	 * 将 json byte array 转成对象
	 *
	 * @param json json
	 * @param type Type
	 * @param <T>  泛型
	 * @return 对象
	 */
	default <T> T readValue(byte[] json, Type type) {
		return readValue(new String(json), type);
	}

	/**
	 * 将 json 字符串转成 list
	 *
	 * @param json  json
	 * @param clazz Class
	 * @param <T>   泛型
	 * @return 对象
	 */
	<T> List<T> readList(String json, Class<T> clazz);

	/**
	 * 将 json byte array 转成 list
	 *
	 * @param json  json
	 * @param clazz Class
	 * @param <T>   泛型
	 * @return 对象
	 */
	default <T> List<T> readList(byte[] json, Class<T> clazz) {
		return readList(new String(json), clazz);
	}

	/**
	 * 将 json 字符串转成 map
	 *
	 * @param json       json
	 * @param keyClass   key Class
	 * @param valueClass value Class
	 * @param <K>        泛型
	 * @param <V>        泛型
	 * @return 对象
	 */
	default <K, V> Map<K, V> readMap(String json, Class<K> keyClass, Class<V> valueClass) {
		return readValue(json, TypeUtils.getGenericType(Map.class, keyClass, valueClass));
	}

	/**
	 * 将 json 字符串转成 map
	 *
	 * @param json       json
	 * @param keyClass   key Class
	 * @param valueClass value Class
	 * @param <K>        泛型
	 * @param <V>        泛型
	 * @return 对象
	 */
	default <K, V> Map<K, V> readMap(byte[] json, Class<K> keyClass, Class<V> valueClass) {
		return readValue(json, TypeUtils.getGenericType(Map.class, keyClass, valueClass));
	}

	/**
	 * 对象类型转换
	 *
	 * @param fromValue   fromValue
	 * @param toValueType toValueType
	 * @param <T>         泛型
	 * @return 对象
	 */
	default <T> T convertValue(Object fromValue, Class<T> toValueType) {
		return this.readValue(this.toJsonBytes(fromValue), toValueType);
	}

	/**
	 * 对象类型转换
	 *
	 * @param fromValue   fromValue
	 * @param toValueType toValueType
	 * @param <T>         泛型
	 * @return 对象
	 */
	default <T> T convertValue(Object fromValue, Type toValueType) {
		return this.readValue(this.toJsonBytes(fromValue), toValueType);
	}
}
