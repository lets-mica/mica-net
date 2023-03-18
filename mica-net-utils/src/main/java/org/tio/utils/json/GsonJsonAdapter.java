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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Objects;

/**
 * FastJson1 json 适配器
 *
 * @author L.cm
 */
public class GsonJsonAdapter implements JsonAdapter {
	/**
	 * 类名
	 */
	public static final String CLAZZ_NAME = "com.google.gson.Gson";

	private final Gson gson;

	public GsonJsonAdapter() {
		this(new Gson());
	}

	public GsonJsonAdapter(Gson gson) {
		this.gson = Objects.requireNonNull(gson, "gson is null.");
	}

	public GsonJsonAdapter(GsonBuilder gsonBuilder) {
		this(Objects.requireNonNull(gsonBuilder, "gsonBuilder is null.").create());
	}

	@Override
	public String toJsonString(Object object) {
		return gson.toJson(object);
	}

	@Override
	public <T> T readValue(String json, Class<T> clazz) {
		return gson.fromJson(json, clazz);
	}

	@Override
	public <T> List<T> readList(String json, Class<T> clazz) {
		return gson.fromJson(json, getListTypeToken(clazz));
	}

	@SuppressWarnings("unchecked")
	private static <T> TypeToken<List<T>> getListTypeToken(Class<T> clazz) {
		return (TypeToken<List<T>>) TypeToken.getParameterized(List.class, clazz);
	}

}
