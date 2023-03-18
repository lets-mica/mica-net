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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tio.utils.mica.ExceptionUtils;

import java.util.List;
import java.util.Objects;

/**
 * FastJson1 json 适配器
 *
 * @author L.cm
 */
public class JacksonJsonAdapter implements JsonAdapter {
	/**
	 * 类名
	 */
	public static final String CLAZZ_NAME = "com.fasterxml.jackson.databind.ObjectMapper";

	private final ObjectMapper objectMapper;

	public JacksonJsonAdapter() {
		this(new ObjectMapper());
	}

	public JacksonJsonAdapter(ObjectMapper objectMapper) {
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is null.");
	}

	@Override
	public String toJsonString(Object object) {
		try {
			return objectMapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw ExceptionUtils.unchecked(e);
		}
	}

	@Override
	public <T> T readValue(String json, Class<T> clazz) {
		try {
			return objectMapper.readValue(json, clazz);
		} catch (JsonProcessingException e) {
			throw ExceptionUtils.unchecked(e);
		}
	}

	@Override
	public <T> List<T> readList(String json, Class<T> clazz) {
		JavaType javaType = objectMapper.getTypeFactory()
			.constructCollectionType(List.class, clazz);
		try {
			return objectMapper.readValue(json, javaType);
		} catch (JsonProcessingException e) {
			throw ExceptionUtils.unchecked(e);
		}
	}

}
