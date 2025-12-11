package org.tio.utils.mica;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 类型工具类
 *
 * @author L.cm
 */
public class TypeUtils {

	/**
	 * 动态构建泛型类型
	 *
	 * @param rawType       原始类型，如 List.class
	 * @param typeArguments 泛型参数，如 String.class
	 * @return type
	 */
	public static Type getGenericType(Class<?> rawType, Type... typeArguments) {
		return new ParameterizedType() {
			@Override
			public Type[] getActualTypeArguments() {
				return typeArguments;
			}

			@Override
			public Type getRawType() {
				return rawType;
			}

			@Override
			public Type getOwnerType() {
				return null;
			}
		};
	}

}
