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

package net.dreamlu.mica.net.utils.hutool;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * class 工具类
 *
 * @author L.cm
 */
public class ClassUtil {
	/**
	 * 添加 MethodHandles.Lookup 实例和字段缓存
 	 */
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	/**
	 * 使用声明类作为键
 	 */
	private static final Map<FieldKey, MethodHandle> FIELD_CACHE = new ConcurrentHashMap<>();
	/**
	 * 字段查找结果缓存
 	 */
	private static final Map<Class<?>, Map<String, Class<?>>> DECLARING_CLASS_CACHE = new ConcurrentHashMap<>();

	/**
	 * 确定class是否可以被加载
	 *
	 * @param className 完整类名
	 * @return {boolean}
	 */
	public static boolean isPresent(String className) {
		try {
			Class.forName(className);
			return true;
		} catch (Throwable ex) {
			return false;
		}
	}

	/**
	 * 查找字段，包括父类中的字段
	 *
	 * @param clazz     类
	 * @param fieldName 字段名
	 * @return 找到的字段，如果没找到返回null
	 */
	private static Class<?> findDeclaringClass(Class<?> clazz, String fieldName) {
		return DECLARING_CLASS_CACHE
			.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>())
			.computeIfAbsent(fieldName, k -> {
				for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
					try {
						current.getDeclaredField(fieldName);
						return current;  // 找到字段的声明类
					} catch (NoSuchFieldException ignored) {
						// 继续向上查找
					}
				}
				return null;  // 没找到
			});
	}

	/**
	 * 获取字段值
	 *
	 * @param obj       obj
	 * @param fieldName fieldName
	 * @return fieldValue
	 */
	public static Object getFieldValue(Object obj, String fieldName) {
		if (obj == null || fieldName == null) {
			return null;
		} else if (obj instanceof Map) {
			return ((Map<?, ?>) obj).get(fieldName);
		}
		try {
			Class<?> clazz = obj.getClass();
			// 1. 查找声明类
			Class<?> declaringClass = findDeclaringClass(clazz, fieldName);
			if (declaringClass == null) {
				throw new NoSuchFieldException("字段 " + fieldName + " 在类 " + clazz.getName() + " 及其父类中不存在");
			}
			// 2. 使用声明类作为缓存键
			FieldKey cacheKey = new FieldKey(declaringClass, fieldName);
			MethodHandle mh = FIELD_CACHE.computeIfAbsent(cacheKey, k -> {
				try {
					Field field = declaringClass.getDeclaredField(fieldName);
					field.setAccessible(true);
					return LOOKUP.unreflectGetter(field);
				} catch (NoSuchFieldException | IllegalAccessException e) {
					// 理论上不应该发生，因为之前已经验证过
					throw new RuntimeException(e);
				}
			});
			return mh.invoke(obj);
		} catch (Throwable e) {
			throw new IllegalArgumentException("解析对象中的字段时发生错误: " + fieldName + "，请检查字段是否存在或类型是否正确", e);
		}
	}

}
