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

import org.noear.snack4.Feature;
import org.noear.snack4.ONode;
import org.noear.snack4.Options;
import org.noear.snack4.codec.TypeRef;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

/**
 * hutool json 适配器
 *
 * @author L.cm
 */
public class Snack4JsonAdapter implements JsonAdapter {
	/**
	 * 类名
	 */
	public static final String CLAZZ_NAME = "org.noear.snack4.ONode";

	private final Options options;

	public Snack4JsonAdapter() {
		this(Options.of());
	}

	public Snack4JsonAdapter(Options options) {
		this.options = Objects.requireNonNull(options, "noear Snack4 json options is null.");
		options.addFeatures(Feature.Write_EnumUsingName);
	}

	@Override
	public boolean isValidJson(String json) {
		try {
			ONode oNode = ONode.ofJson(json, options);
			return oNode != null && (oNode.isObject() || oNode.isArray());
		} catch (Throwable e) {
			return false;
		}
	}

	@Override
	public String toJsonString(Object object) {
		return ONode.ofBean(object, options).toJson();
	}

	@Override
	public <T> T readValue(String json, Class<T> clazz) {
		return ONode.ofJson(json, options).toBean(clazz);
	}

	@Override
	public <T> T readValue(String json, Type type) {
		return ONode.ofJson(json, options).toBean(type);
	}

	@Override
	public <T> List<T> readList(String json, Class<T> clazz) {
		return ONode.ofJson(json, options).toBean(TypeRef.listOf(clazz));
	}

	@Override
	public <T> T convertValue(Object fromValue, Class<T> toValueType) {
		return ONode.ofBean(fromValue, options).toBean(toValueType);
	}

	@Override
	public <T> T convertValue(Object fromValue, Type toValueType) {
		return ONode.ofBean(fromValue, options).toBean(toValueType);
	}

}
