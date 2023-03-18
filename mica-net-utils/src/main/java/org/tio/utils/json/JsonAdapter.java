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
 * json 适配器
 *
 * @author L.cm
 */
public interface JsonAdapter {

	/**
	 * 转换成 json 字符串
	 *
	 * @param object object
	 * @return json 字符串
	 */
	String toJsonString(Object object);

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
	 * @param json  json
	 * @param clazz Class
	 * @param <T>   泛型
	 * @return 对象
	 */
	<T> List<T> readList(String json, Class<T> clazz);

	/**
	 * 获取 jsonAdapter
	 *
	 * @return JsonAdapter
	 */
	static JsonAdapter getJsonAdapter() {
		return getJsonAdapter(null);
	}

	/**
	 * 获取 jsonAdapter
	 *
	 * @param jsonAdapter JsonAdapter
	 * @return JsonAdapter
	 */
	static JsonAdapter getJsonAdapter(JsonAdapter jsonAdapter) {
		if (jsonAdapter != null) {
			return jsonAdapter;
		}
		// 基于 jackson
		if (ClassUtil.isPresent(JacksonJsonAdapter.CLAZZ_NAME)) {
			return new JacksonJsonAdapter();
		}
		// 基于 fastjson2
		if (ClassUtil.isPresent(FastJson2JsonAdapter.CLAZZ_NAME)) {
			return new FastJson2JsonAdapter();
		}
		// 基于 fastjson
		if (ClassUtil.isPresent(FastJson1JsonAdapter.CLAZZ_NAME)) {
			return new FastJson1JsonAdapter();
		}
		// 基于 gson
		if (ClassUtil.isPresent(GsonJsonAdapter.CLAZZ_NAME)) {
			return new GsonJsonAdapter();
		}
		// 基于 hutool
		if (ClassUtil.isPresent(HuToolJsonAdapter.CLAZZ_NAME)) {
			return new HuToolJsonAdapter();
		}
		throw new IllegalArgumentException("Args jsonAdapter is null and there is no available JSON toolkits (Jackson, Fastjson1, Fastjson2, Gson, Hutool-json)");
	}

}
