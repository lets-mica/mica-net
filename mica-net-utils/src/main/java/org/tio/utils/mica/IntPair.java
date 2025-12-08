package org.tio.utils.mica;

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
	public String toString() {
		return "IntPair{" +
			"key=" + key +
			", value=" + value +
			'}';
	}
}
