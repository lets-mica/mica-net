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

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;
import java.util.List;

/**
 * jackson json 适配器
 *
 * @author L.cm
 */
public class Jackson3JsonAdapter implements JsonAdapter {
	/**
	 * 类名
	 */
	public static final String CLAZZ_NAME = "tools.jackson.databind.json.JsonMapper";

	private final JsonMapper jsonMapper;

	public Jackson3JsonAdapter() {
		this(new JsonMapper());
	}

	public Jackson3JsonAdapter(JsonFactoryBuilder builder) {
		this(builder.build());
	}

	public Jackson3JsonAdapter(JsonFactory factory) {
		this(JsonMapper.builder(factory));
	}

	public Jackson3JsonAdapter(JsonMapper.Builder builder) {
		this.jsonMapper = builder.findAndAddModules()
			.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
			.changeDefaultPropertyInclusion(handler -> handler.withValueInclusion(JsonInclude.Include.NON_ABSENT))
			// 启用全局忽略未知属性
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.build();
	}

	public Jackson3JsonAdapter(JsonMapper jsonMapper) {
		// 使用传入的 jsonMapper rebuild，避免污染源 jsonMapper
		this(jsonMapper.rebuild());
	}

	@Override
	public boolean isValidJson(String json) {
		try {
			JsonNode jsonNode = jsonMapper.readTree(json);
			// 严格校验：只有是 JSON对象 或 JSON数组 时才返回 true
			return jsonNode != null && (jsonNode.isObject() || jsonNode.isArray());
		} catch (Throwable e) {
			return false;
		}
	}

	@Override
	public String toJsonString(Object object) {
		return jsonMapper.writeValueAsString(object);
	}

	@Override
	public byte[] toJsonBytes(Object object) {
		return jsonMapper.writeValueAsBytes(object);
	}

	@Override
	public <T> T readValue(String json, Class<T> clazz) {
		return jsonMapper.readValue(json, clazz);
	}

	@Override
	public <T> T readValue(String json, Type type) {
		return jsonMapper.readValue(json, getJavaType(type));
	}

	@Override
	public <T> T readValue(byte[] json, Class<T> clazz) {
		return jsonMapper.readValue(json, clazz);
	}

	@Override
	public <T> T readValue(byte[] json, Type type) {
		return jsonMapper.readValue(json, getJavaType(type));
	}

	@Override
	public <T> List<T> readList(String json, Class<T> clazz) {
		JavaType javaType = jsonMapper.getTypeFactory()
			.constructCollectionType(List.class, clazz);
		return jsonMapper.readValue(json, javaType);
	}

	@Override
	public <T> List<T> readList(byte[] json, Class<T> clazz) {
		JavaType javaType = jsonMapper.getTypeFactory()
			.constructCollectionType(List.class, clazz);
		return jsonMapper.readValue(json, javaType);
	}

	@Override
	public <T> T convertValue(Object fromValue, Class<T> toValueType) {
		return jsonMapper.convertValue(fromValue, toValueType);
	}

	@Override
	public <T> T convertValue(Object fromValue, Type toValueType) {
		return jsonMapper.convertValue(fromValue, getJavaType(toValueType));
	}

	private JavaType getJavaType(Type type) {
		return jsonMapper.getTypeFactory().constructType(type);
	}
}
