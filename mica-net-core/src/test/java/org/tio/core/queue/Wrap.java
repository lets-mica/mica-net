package org.tio.core.queue;

import java.util.Objects;

public final class Wrap<E> {
	private E value;

	public Wrap() {
	}

	public Wrap(E value) {
		this.value = value;
	}

	static <E> Wrap<E> of(E src) {
		return new Wrap<>(src);
	}


	public E getValue() {
		return value;
	}

	public void setValue(E value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Wrap)) return false;
		Wrap<?> wrap = (Wrap<?>) o;
		return Objects.equals(value, wrap.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return "Wrap{" +
			"value=" + value +
			'}';
	}
}
