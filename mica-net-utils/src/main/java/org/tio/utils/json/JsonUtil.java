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

import org.tio.utils.hutool.ClassUtil;

import java.util.List;

/**
 * json 工具
 *
 * @author L.cm
 */
public class JsonUtil {
	private static JsonAdapter jsonAdapter;

	/**
	 * 获取 jsonAdapter
	 *
	 * @return JsonAdapter
	 */
	public static JsonAdapter getJsonAdapter() {
		return jsonAdapter == null ? getJsonAdapter(null) : jsonAdapter;
	}

	/**
	 * 获取 jsonAdapter
	 *
	 * @param jsonAdapter JsonAdapter
	 * @return JsonAdapter
	 */
	public static JsonAdapter getJsonAdapter(JsonAdapter jsonAdapter) {
		JsonAdapter defaultJsonAdapter;
		if (jsonAdapter != null) {
			defaultJsonAdapter = jsonAdapter;
		} else if (ClassUtil.isPresent(JacksonJsonAdapter.CLAZZ_NAME)) {
			defaultJsonAdapter = new JacksonJsonAdapter();
		} else if (ClassUtil.isPresent(FastJson2JsonAdapter.CLAZZ_NAME)) {
			defaultJsonAdapter = new FastJson2JsonAdapter();
		} else if (ClassUtil.isPresent(GsonJsonAdapter.CLAZZ_NAME)) {
			defaultJsonAdapter = new GsonJsonAdapter();
		} else if (ClassUtil.isPresent(FastJson1JsonAdapter.CLAZZ_NAME)) {
			defaultJsonAdapter = new FastJson1JsonAdapter();
		} else if (ClassUtil.isPresent(HuToolJsonAdapter.CLAZZ_NAME)) {
			defaultJsonAdapter = new HuToolJsonAdapter();
		} else if (ClassUtil.isPresent(Snack3JsonAdapter.CLAZZ_NAME)) {
			defaultJsonAdapter = new Snack3JsonAdapter();
		} else {
			throw new IllegalArgumentException("Args jsonAdapter is null and there is no available JSON toolkits (Jackson, Fastjson1, Fastjson2, Gson, Hutool-json or Snack3)");
		}
		JsonUtil.jsonAdapter = defaultJsonAdapter;
		return defaultJsonAdapter;
	}


	/**
	 * 转换成 json 字符串
	 *
	 * @param object object
	 * @return json 字符串
	 */
	public static String toJsonString(Object object) {
		return getJsonAdapter().toJsonString(object);
	}

	/**
	 * 转换成 json byte array
	 *
	 * @param object object
	 * @return json 字符串
	 */
	public static byte[] toJsonBytes(Object object) {
		return getJsonAdapter().toJsonBytes(object);
	}

	/**
	 * 将 json 字符串转成对象
	 *
	 * @param json  json
	 * @param clazz Class
	 * @param <T>   泛型
	 * @return 对象
	 */
	public static <T> T readValue(String json, Class<T> clazz) {
		return getJsonAdapter().readValue(json, clazz);
	}

	/**
	 * 将 json byte array 转成对象
	 *
	 * @param json  json
	 * @param clazz Class
	 * @param <T>   泛型
	 * @return 对象
	 */
	public static <T> T readValue(byte[] json, Class<T> clazz) {
		return getJsonAdapter().readValue(json, clazz);
	}

	/**
	 * 将 json 字符串转成对象集合
	 *
	 * @param json  json
	 * @param clazz Class
	 * @param <T>   泛型
	 * @return 对象
	 */
	public static <T> List<T> readList(String json, Class<T> clazz) {
		return getJsonAdapter().readList(json, clazz);
	}

	/**
	 * 将 json byte array 转成对象集合
	 *
	 * @param json  json
	 * @param clazz Class
	 * @param <T>   泛型
	 * @return 对象
	 */
	public static <T> List<T> readList(byte[] json, Class<T> clazz) {
		return getJsonAdapter().readList(json, clazz);
	}

}
