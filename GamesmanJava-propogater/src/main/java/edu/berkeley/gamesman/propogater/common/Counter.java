package edu.berkeley.gamesman.propogater.common;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class Counter<T> implements Iterable<T> {
	private final HashMap<T, Integer> hm = new HashMap<T, Integer>();

	public boolean has(T t) {
		return get(t) > 0;
	}

	public void add(T t) {
		hm.put(t, get(t) + 1);
	}

	public void remove(T t) {
		Integer res = get(t);
		if (res == 0)
			throw new NoSuchElementException("Count for " + t
					+ " cannot be negative");
		hm.put(t, res - 1);
	}

	public boolean isEmpty(T t) {
		return get(t) == 0;
	}

	private Integer get(T t) {
		Integer result = hm.get(t);
		if (result == null) {
			return 0;
		} else
			return result;
	}

	@Override
	public Iterator<T> iterator() {
		return new CounterIterator();
	}

	private class CounterIterator implements Iterator<T> {
		private final Iterator<Map.Entry<T, Integer>> iter = hm.entrySet()
				.iterator();
		private Map.Entry<T, Integer> next;

		public CounterIterator() {
			nextOrNull();
		}

		@Override
		public boolean hasNext() {
			while (next != null && next.getValue() == 0) {
				nextOrNull();
			}
			return next != null;
		}

		@Override
		public T next() throws NoSuchElementException {
			if (hasNext()) {
				T result = next.getKey();
				nextOrNull();
				return result;
			} else
				throw new NoSuchElementException();
		}

		private void nextOrNull() {
			next = iter.hasNext() ? iter.next() : null;
		}

		@Override
		public void remove() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}
	}
}