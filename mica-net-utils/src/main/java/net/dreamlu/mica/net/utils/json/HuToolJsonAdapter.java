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

package net.dreamlu.mica.net.utils.json;

import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

/**
 * hutool json 适配器
 *
 * @author L.cm
 */
public class HuToolJsonAdapter implements JsonAdapter {
	/**
	 * 类名
	 */
	public static final String CLAZZ_NAME = "cn.hutool.json.JSONUtil";

	private final JSONConfig jsonConfig;

	public HuToolJsonAdapter() {
		this(JSONConfig.create());
	}

	public HuToolJsonAdapter(JSONConfig jsonConfig) {
		this.jsonConfig = Objects.requireNonNull(jsonConfig, "HuTool json jsonConfig is null.");
	}

	@Override
	public boolean isValidJson(String json) {
		return JSONUtil.isTypeJSON(json);
	}

	@Override
	public String toJsonString(Object object) {
		return JSONUtil.toJsonStr(object, jsonConfig);
	}

	@Override
	public <T> T readValue(String json, Class<T> clazz) {
		return JSONUtil.parseObj(json, jsonConfig).toBean(clazz);
	}

	@Override
	public <T> T readValue(String json, Type type) {
		return JSONUtil.parseObj(json, jsonConfig).toBean(type);
	}

	@Override
	public <T> List<T> readList(String json, Class<T> clazz) {
		return JSONUtil.parseArray(json, jsonConfig).toList(clazz);
	}

	@Override
	public <T> T convertValue(Object fromValue, Class<T> toValueType) {
		return JSONUtil.parse(fromValue, jsonConfig).toBean(toValueType);
	}

	@Override
	public <T> T convertValue(Object fromValue, Type toValueType) {
		return JSONUtil.parse(fromValue, jsonConfig).toBean(toValueType);
	}

}
