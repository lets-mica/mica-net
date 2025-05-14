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

import org.noear.snack.ONode;
import org.noear.snack.core.Feature;
import org.noear.snack.core.Options;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

/**
 * hutool json 适配器
 *
 * @author L.cm
 */
public class Snack3JsonAdapter implements JsonAdapter {
	/**
	 * 类名
	 */
	public static final String CLAZZ_NAME = "org.noear.snack.ONode";

	private final Options options;

	public Snack3JsonAdapter() {
		this(Options.def());
	}

	public Snack3JsonAdapter(Options options) {
		this.options = Objects.requireNonNull(options, "noear Snack3 json options is null.");
		this.options.add(Feature.EnumUsingName);
	}

	@Override
	public String toJsonString(Object object) {
		return ONode.load(object, options).toJson();
	}

	@Override
	public <T> T readValue(String json, Class<T> clazz) {
		return ONode.loadStr(json, options).toObject(clazz);
	}

	@Override
	public <T> T readValue(String json, Type type) {
		return ONode.loadStr(json, options).toObject(type);
	}

	@Override
	public <T> List<T> readList(String json, Class<T> clazz) {
		return ONode.loadStr(json, options).toObjectList(clazz);
	}

}
