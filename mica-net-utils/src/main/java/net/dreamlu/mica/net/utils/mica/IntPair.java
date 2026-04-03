package net.dreamlu.mica.net.utils.mica;

import java.util.Objects;

/**
 * IntPair
 *
 * @param <V> 泛型
 * @author L.cm
 */
public class IntPair<V> {
	private final int key;
	private final V value;

	public IntPair(int key, V value) {
		this.key = key;
		this.value = value;
	}

	public int getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		IntPair<?> intPair = (IntPair<?>) o;
		return key == intPair.key && Objects.equals(value, intPair.value);
	}

	@Override
	public int hashCode() {
		int result = key;
		result = 31 * result + Objects.hashCode(value);
		return result;
	}

	@Override
	public String toString() {
		return "IntPair{" +
			"key=" + key +
			", value=" + value +
			'}';
	}
}
