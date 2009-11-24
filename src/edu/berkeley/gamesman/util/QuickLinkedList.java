package edu.berkeley.gamesman.util;

import java.lang.reflect.Array;
import java.util.*;

public class QuickLinkedList<T> implements List<T>, Queue<T> {
	private final int[] nextList, prevList;

	private int first, last, size = 0, lastAdded = 0;

	private final T[] objects;

	private final int[] nextNull;

	private int firstNull;

	private final QuickLinkedIterator internalIterator;

	private final Factory<T> factory;

	public QuickLinkedList(T[] objects, Factory<T> factory) {
		nextList = new int[objects.length];
		prevList = new int[objects.length];
		nextNull = new int[objects.length];
		firstNull = 0;
		this.objects = objects;
		for (int i = 0; i < objects.length; i++) {
			nextList[i] = -1;
			prevList[i] = -1;
			nextNull[i] = i + 1;
		}
		nextNull[objects.length - 1] = -1;
		internalIterator = listIterator();
		this.factory = factory;
	}

	private boolean addBefore(int position) {
		lastAdded = nextNullAndRemove();
		if (lastAdded < 0)
			return false;
		nextList[lastAdded] = position;
		if (position < 0) {
			prevList[lastAdded] = last;
			if (last < 0)
				first = lastAdded;
			else
				nextList[last] = lastAdded;
			last = lastAdded;
		} else {
			prevList[lastAdded] = prevList[position];
			if (first == position)
				first = lastAdded;
			else
				nextList[prevList[position]] = lastAdded;
			prevList[position] = lastAdded;
		}
		++size;
		return true;
	}

	private int nextNullAndRemove() {
		int retVal = firstNull;
		firstNull = nextNull[firstNull];
		return retVal;
	}

	private void removeFrom(int position, boolean hold) {
		if (first == position)
			first = nextList[position];
		else
			nextList[prevList[position]] = nextList[position];
		if (last == position)
			last = prevList[position];
		else
			prevList[nextList[position]] = prevList[position];
		--size;
		if (!hold)
			addNull(position);
	}

	private void addNull(int position) {
		nextNull[position] = firstNull;
		firstNull = position;
	}

	public class QuickLinkedIterator implements ListIterator<T> {
		int nextPosition = first;

		boolean lastCallWasPrevious = false;

		private QuickLinkedIterator() {
			nextPosition = first;
			lastCallWasPrevious = false;
		}

		public void add(T e) {
			addBefore(nextPosition);
			objects[lastAdded] = e;
		}

		public T add() {
			addBefore(nextPosition);
			if (objects[lastAdded] == null)
				objects[lastAdded] = factory.newElement();
			return objects[lastAdded];
		}

		public boolean hasNext() {
			return nextPosition > 0;
		}

		public boolean hasPrevious() {
			if (nextPosition > 0)
				return prevList[nextPosition] > 0;
			else
				return last > 0;
		}

		public T next() {
			T retVal = objects[nextPosition];
			nextPosition = nextList[nextPosition];
			lastCallWasPrevious = false;
			return retVal;
		}

		public int nextIndex() {
			throw new UnsupportedOperationException();
		}

		public T previous() {
			if (nextPosition > 0)
				nextPosition = prevList[nextPosition];
			else
				nextPosition = last;
			lastCallWasPrevious = true;
			return objects[nextPosition];
		}

		public int previousIndex() {
			throw new UnsupportedOperationException();
		}

		public void remove() {
			if (lastCallWasPrevious)
				removeFrom(nextPosition, false);
			else {
				if (nextPosition > 0)
					removeFrom(prevList[nextPosition], false);
				else
					removeFrom(last, false);
			}
		}

		public void set(T e) {
			if (lastCallWasPrevious)
				objects[nextPosition] = e;
			else {
				if (nextPosition > 0)
					objects[prevList[nextPosition]] = e;
				else
					objects[last] = e;
			}
		}

		public void reset(boolean fromEnd) {
			if (fromEnd) {
				nextPosition = -1;
				lastCallWasPrevious = true;
			} else {
				nextPosition = first;
				lastCallWasPrevious = false;
			}
		}

		public int nextSerial() {
			return nextPosition;
		}

		public void jumpSerial(int serial) {
			nextPosition = serial;
		}

		public int tempRemove() {
			int posRemoved;
			if (lastCallWasPrevious)
				posRemoved = nextPosition;
			else {
				if (nextPosition > 0)
					posRemoved = prevList[nextPosition];
				else
					posRemoved = last;
			}
			removeFrom(posRemoved, true);
			return posRemoved;
		}
	}

	public boolean add(T e) {
		boolean worked = addBefore(-1);
		if (worked) {
			objects[lastAdded] = e;
			return true;
		} else
			return false;
	}

	public void add(int index, T element) {
		if (index == 0) {
			addBefore(first);
			objects[lastAdded] = element;
		} else if (index == size) {
			add(element);
		} else {
			setNextIndex(internalIterator, index);
			internalIterator.add(element);
		}
	}

	public boolean addAll(Collection<? extends T> c) {
		Iterator<? extends T> it = c.iterator();
		boolean changed = false;
		while (it.hasNext())
			if (add(it.next()))
				changed = true;
			else
				return changed;
		return changed;
	}

	public boolean addAll(int index, Collection<? extends T> c) {
		if (index == size)
			return addAll(c);
		else {
			Iterator<? extends T> it = c.iterator();
			setNextIndex(internalIterator, index);
			while (it.hasNext())
				internalIterator.add(it.next());
			return true;
		}
	}

	public void clear() {
		while (size > 0)
			remove();
	}

	public boolean contains(Object o) {
		return find(internalIterator, o);
	}

	public boolean containsAll(Collection<?> c) {
		for (Object o : c)
			if (!contains(o))
				return false;
		return true;
	}

	public T get(int index) {
		setNextIndex(internalIterator, index);
		return internalIterator.next();
	}

	public int indexOf(Object o) {
		internalIterator.reset(false);
		int count = 0;
		while (internalIterator.hasNext()) {
			if (internalIterator.next().equals(o))
				return count;
			else
				count++;
		}
		return -1;
	}

	public boolean isEmpty() {
		return size <= 0;
	}

	public Iterator<T> iterator() {
		return listIterator();
	}

	public int lastIndexOf(Object o) {
		internalIterator.reset(true);
		int count = size - 1;
		while (internalIterator.hasPrevious()) {
			if (internalIterator.previous().equals(o))
				return count;
			else
				count--;
		}
		return -1;
	}

	public QuickLinkedIterator listIterator() {
		return new QuickLinkedIterator();
	}

	public ListIterator<T> listIterator(int index) {
		QuickLinkedIterator retVal = listIterator();
		setNextIndex(retVal, index);
		return retVal;
	}

	private void setNextIndex(QuickLinkedIterator iter, int index) {
		if (size - index < index) {
			iter.reset(true);
			for (int i = size; i >= index; i--)
				iter.previous();
		} else {
			iter.reset(false);
			for (int i = 0; i < index; i++)
				iter.next();
		}
	}

	public boolean remove(Object o) {
		if (find(internalIterator, o)) {
			internalIterator.next();
			internalIterator.remove();
			return true;
		} else
			return false;
	}

	private boolean find(QuickLinkedIterator iter, Object o) {
		int current = iter.nextPosition;
		while (iter.hasNext())
			if (iter.next().equals(o)) {
				iter.previous();
				return true;
			}
		iter.reset(false);
		while (iter.nextPosition != current)
			if (iter.next().equals(o)) {
				iter.previous();
				return true;
			}
		return false;
	}

	public T remove(int index) {
		setNextIndex(internalIterator, index);
		T retVal = internalIterator.next();
		internalIterator.remove();
		return retVal;
	}

	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for (Object o : c) {
			if (remove(o))
				changed = true;
		}
		return changed;
	}

	public boolean retainAll(Collection<?> c) {
		internalIterator.reset(false);
		boolean changed = false;
		while (internalIterator.hasNext())
			if (!c.contains(internalIterator.next())) {
				changed = true;
				internalIterator.remove();
			}
		return changed;
	}

	public T set(int index, T element) {
		setNextIndex(internalIterator, index);
		T retVal = internalIterator.next();
		internalIterator.set(element);
		return retVal;
	}

	public int size() {
		return size;
	}

	public QuickLinkedList<T> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}

	public Object[] toArray() {
		Object[] retArr = new Object[size];
		internalIterator.reset(false);
		for (int i = 0; internalIterator.hasNext(); i++)
			retArr[i] = internalIterator.next();
		return retArr;
	}

	public <S> S[] toArray(S[] a) {
		if (a.length < size)
			a = Util.checkedCast(Array.newInstance(a.getClass()
					.getComponentType(), size));
		internalIterator.reset(false);
		Iterator<S> iter = Util.checkedCast(internalIterator);
		for (int i = 0; iter.hasNext(); i++)
			a[i] = iter.next();
		return a;
	}

	public T element() {
		T result = peek();
		if (result == null)
			throw new NoSuchElementException("List is empty");
		else
			return result;
	}

	public boolean offer(T e) {
		return add(e);
	}

	public T peek() {
		if (size <= 0)
			return null;
		else
			return objects[first];
	}

	public T poll() {
		if (first < 0)
			return null;
		T result = objects[first];
		removeFrom(first, false);
		return result;
	}

	public T remove() {
		T result = poll();
		if (result == null)
			throw new NoSuchElementException("List is empty");
		else
			return result;
	}

	public void putBack(int serial) {
		if (nextList[serial] < 0) {
			if (last < 0)
				first = serial;
			else {
				nextList[last] = serial;
				prevList[serial] = last;
			}
			last = serial;
		} else {
			prevList[serial] = prevList[nextList[serial]];
			prevList[nextList[serial]] = serial;
			if (prevList[serial] < 0) {
				first = serial;
			} else
				nextList[prevList[serial]] = serial;
		}
		++size;
	}

	public T add() {
		addBefore(first);
		if (objects[lastAdded] == null)
			objects[lastAdded] = factory.newElement();
		return objects[lastAdded];
	}
}
