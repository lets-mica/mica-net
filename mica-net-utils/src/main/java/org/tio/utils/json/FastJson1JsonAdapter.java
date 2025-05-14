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

import com.alibaba.fastjson.JSON;

import java.lang.reflect.Type;
import java.util.List;

/**
 * FastJson1 json 适配器
 *
 * @author L.cm
 */
public class FastJson1JsonAdapter implements JsonAdapter {
	/**
	 * 类名
	 */
	public static final String CLAZZ_NAME = "com.alibaba.fastjson.JSON";

	@Override
	public String toJsonString(Object object) {
		return JSON.toJSONString(object);
	}

	@Override
	public byte[] toJsonBytes(Object object) {
		return JSON.toJSONBytes(object);
	}

	@Override
	public <T> T readValue(String json, Class<T> clazz) {
		return JSON.parseObject(json, clazz);
	}

	@Override
	public <T> T readValue(String json, Type type) {
		return JSON.parseObject(json, type);
	}

	@Override
	public <T> T readValue(byte[] json, Class<T> clazz) {
		return JSON.parseObject(json, clazz);
	}

	@Override
	public <T> T readValue(byte[] json, Type type) {
		return JSON.parseObject(json, type);
	}

	@Override
	public <T> List<T> readList(String json, Class<T> clazz) {
		return JSON.parseArray(json, clazz);
	}

}
