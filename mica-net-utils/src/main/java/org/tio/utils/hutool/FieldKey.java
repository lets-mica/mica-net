package org.tio.utils.hutool;

import java.util.Objects;

/**
 * 字段缓存键，用于缓存反射字段的 MethodHandle
 *
 * @author L.cm
 */
final class FieldKey {
	private final Class<?> clazz;
	private final String fieldName;
	// 缓存哈希值，避免重复计算
	private final int hashCode;

	public FieldKey(Class<?> clazz, String fieldName) {
		this.clazz = Objects.requireNonNull(clazz, "clazz 不能为 null");
		this.fieldName = Objects.requireNonNull(fieldName, "fieldName 不能为 null");
		this.hashCode = calculateHashCode();
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public String getFieldName() {
		return fieldName;
	}

	private int calculateHashCode() {
		// 31 * clazz.hashCode() + fieldName.hashCode()
		// Class.hashCode() 是系统哈希，fieldName 是字符串哈希
		return 31 * clazz.hashCode() + fieldName.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		FieldKey fieldKey = (FieldKey) o;
		// Class 对象用 == 比较，因为同一个类加载器的 Class 是单例
		// 这比 equals 更快
		if (clazz != fieldKey.clazz) {
			return false;
		}
		return fieldName.equals(fieldKey.fieldName);
	}

	@Override
	public int hashCode() {
		// 返回预计算的哈希值
		return hashCode;
	}

	@Override
	public String toString() {
		return clazz.getName() + '#' + fieldName;
	}
}
