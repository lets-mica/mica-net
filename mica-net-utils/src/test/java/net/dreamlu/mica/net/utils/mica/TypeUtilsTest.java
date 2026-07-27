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

package net.dreamlu.mica.net.utils.mica;

import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TypeUtils 单元测试
 *
 * @author L.cm
 */
class TypeUtilsTest {

	@Test
	void testSimpleGenericType() {
		Type type = TypeUtils.getGenericType(List.class, String.class);
		assertNotNull(type);
		assertTrue(type instanceof ParameterizedType);

		ParameterizedType parameterizedType = (ParameterizedType) type;
		assertEquals(List.class, parameterizedType.getRawType());
		assertArrayEquals(new Type[]{String.class}, parameterizedType.getActualTypeArguments());
		assertNull(parameterizedType.getOwnerType());
	}

	@Test
	void testNestedGenericType() {
		Type innerType = TypeUtils.getGenericType(Map.class, String.class, String.class);
		Type type = TypeUtils.getGenericType(List.class, innerType);
		assertNotNull(type);
		assertTrue(type instanceof ParameterizedType);

		ParameterizedType parameterizedType = (ParameterizedType) type;
		assertEquals(List.class, parameterizedType.getRawType());

		Type[] typeArgs = parameterizedType.getActualTypeArguments();
		assertEquals(1, typeArgs.length);
		assertTrue(typeArgs[0] instanceof ParameterizedType);

		ParameterizedType mapType = (ParameterizedType) typeArgs[0];
		assertEquals(Map.class, mapType.getRawType());
		assertArrayEquals(new Type[]{String.class, String.class}, mapType.getActualTypeArguments());
	}

	@Test
	void testNoTypeArguments() {
		Type type = TypeUtils.getGenericType(String.class);
		assertNotNull(type);
		assertTrue(type instanceof ParameterizedType);

		ParameterizedType parameterizedType = (ParameterizedType) type;
		assertEquals(String.class, parameterizedType.getRawType());
		assertEquals(0, parameterizedType.getActualTypeArguments().length);
	}
}