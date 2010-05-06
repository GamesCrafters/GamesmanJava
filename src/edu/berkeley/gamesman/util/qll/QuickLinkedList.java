package edu.berkeley.gamesman.util.qll;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.ListIterator;

public class QuickLinkedList<T> implements List<T>, Deque<T> {

	public class QLLIterator implements ListIterator<T> {
		Node<T> nextNode;
		boolean lastCallWasPrevious;

		QLLIterator() {
			nextNode = beforeFirst.next;
			lastCallWasPrevious = false;
		}

		public void add(T e) {
			tempAdd(e);
		}

		public Node<T> tempAdd(T e) {
			Node<T> addIn = pool.getNode();
			addIn.object = e;
			addIn.previous = nextNode.previous;
			addIn.next = nextNode;
			addIn.previous.next = addIn;
			addIn.next.previous = addIn;
			++size;
			return addIn;
		}

		public boolean hasNext() {
			return nextNode != afterLast;
		}

		public boolean hasPrevious() {
			return nextNode.previous != beforeFirst;
		}

		public T next() {
			lastCallWasPrevious = false;
			T result = nextNode.object;
			nextNode = nextNode.next;
			return result;
		}

		public int nextIndex() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException();
		}

		public T previous() {
			lastCallWasPrevious = true;
			nextNode = nextNode.previous;
			return nextNode.object;
		}

		public int previousIndex() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException();
		}

		public void remove() {
			pool.giveBack(tempRemove());
		}

		public Node<T> tempRemove() {
			Node<T> rmNode;
			if (lastCallWasPrevious) {
				rmNode = nextNode;
				nextNode = nextNode.next;
			} else
				rmNode = nextNode.previous;
			removeNode(rmNode);
			return rmNode;
		}

		public void set(T e) {
			if (lastCallWasPrevious) {
				nextNode.object = e;
			} else
				nextNode.previous.object = e;
		}

		void toEnd() {
			nextNode = afterLast;
		}

		void toStart() {
			nextNode = beforeFirst.next;
		}

		public void toIndex(int index) {
			if (size - index < index) {
				toEnd();
				for (int i = size - 1; i >= index; --i) {
					previous();
				}
			} else {
				toStart();
				for (int i = 0; i < index; i++)
					next();
			}
		}

		public void toNode(Node<T> serial) {
			nextNode = serial;
		}

		public Node<T> nextSerial() {
			return nextNode;
		}
	}

	private final Node<T> beforeFirst, afterLast;
	private final QLLNullPool<T> pool;
	int size;
	final QLLIterator myIterator;

	public QuickLinkedList() {
		this(new QLLNullPool<T>());
	}

	QuickLinkedList(QLLNullPool<T> pool) {
		size = 0;
		beforeFirst = new Node<T>();
		afterLast = new Node<T>();
		beforeFirst.next = afterLast;
		afterLast.previous = beforeFirst;
		myIterator = listIterator();
		this.pool = pool;
	}

	public void addBack(Node<T> n) {
		n.next.previous = n;
		n.previous.next = n;
		size++;
	}

	public void remove(Node<T> n) {
		removeNode(n);
		pool.giveBack(n);
	}

	void removeNode(Node<T> n) {
		n.next.previous = n.previous;
		n.previous.next = n.next;
		size--;
	}

	public boolean add(T e) {
		myIterator.toEnd();
		myIterator.add(e);
		return true;
	}

	public void add(int index, T element) {
		myIterator.toIndex(index);
		myIterator.add(element);
	}

	public boolean addAll(Collection<? extends T> c) {
		myIterator.toEnd();
		for (T t : c)
			myIterator.add(t);
		return c.size() > 0;
	}

	public boolean addAll(int index, Collection<? extends T> c) {
		myIterator.toIndex(index);
		for (T t : c)
			myIterator.add(t);
		return c.size() > 0;
	}

	public void clear() {
		if (size > 0) {
			pool.giveBack(beforeFirst.next, afterLast.previous);
			beforeFirst.next = afterLast;
			afterLast.previous = beforeFirst;
			size = 0;
		}
	}

	public boolean contains(Object o) {
		myIterator.toStart();
		while (myIterator.hasNext())
			if (myIterator.next().equals(o))
				return true;
		return false;
	}

	public boolean containsAll(Collection<?> c) {
		for (Object o : c)
			if (!contains(o))
				return false;
		return true;
	}

	public T get(int index) {
		myIterator.toIndex(index);
		return myIterator.next();
	}

	public int indexOf(Object o) {
		myIterator.toStart();
		int i = 0;
		while (myIterator.hasNext())
			if (myIterator.next().equals(o))
				return i;
			else
				i++;
		return -1;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public QLLIterator iterator() {
		return listIterator();
	}

	public int lastIndexOf(Object o) {
		myIterator.toEnd();
		int i = size - 1;
		while (myIterator.hasPrevious())
			if (myIterator.previous().equals(o))
				return i;
			else
				i--;
		return -1;
	}

	public QLLIterator listIterator() {
		return new QLLIterator();
	}

	public QLLIterator listIterator(int index) {
		QLLIterator qll = listIterator();
		qll.toIndex(index);
		return qll;
	}

	public boolean remove(Object o) {
		myIterator.toStart();
		while (myIterator.hasNext())
			if (myIterator.next().equals(o)) {
				myIterator.remove();
				return true;
			}
		return false;
	}

	public T remove(int index) {
		myIterator.toIndex(index);
		T result = myIterator.next();
		myIterator.remove();
		return result;
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
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	public T set(int index, T element) {
		myIterator.toIndex(index);
		T last = myIterator.next();
		myIterator.set(element);
		return last;
	}

	public int size() {
		return size;
	}

	public List<T> subList(int fromIndex, int toIndex) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	public Object[] toArray() {
		Object[] objs = new Object[size];
		myIterator.toStart();
		int i = 0;
		while (myIterator.hasNext())
			objs[i++] = myIterator.next();
		return objs;
	}

	@SuppressWarnings( { "unchecked", "hiding" })
	public <T> T[] toArray(T[] a) {
		if (a.length >= size) {
			for (int i = size; i < a.length; i++)
				a[i] = null;
		} else
			a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
		int i = 0;
		myIterator.toStart();
		while (myIterator.hasNext())
			a[i++] = (T) myIterator.next();
		return a;
	}

	public void addFirst(T e) {
		myIterator.toStart();
		myIterator.add(e);
	}

	public void addLast(T e) {
		add(e);
	}

	public QLLIterator descendingIterator() {
		QLLIterator qll = listIterator();
		qll.toEnd();
		return qll;
	}

	public T element() {
		return getFirst();
	}

	public T getFirst() {
		if (size == 0)
			throw new NullPointerException("List is empty");
		myIterator.toStart();
		return myIterator.next();
	}

	public T getLast() {
		if (size == 0)
			throw new NullPointerException("List is empty");
		myIterator.toEnd();
		return myIterator.previous();
	}

	public boolean offer(T e) {
		return offerLast(e);
	}

	public boolean offerFirst(T e) {
		addFirst(e);
		return true;
	}

	public boolean offerLast(T e) {
		addLast(e);
		return true;
	}

	public T peek() {
		return peekFirst();
	}

	public T peekFirst() {
		if (size == 0)
			return null;
		else
			return getFirst();
	}

	public T peekLast() {
		if (size == 0)
			return null;
		else
			return getLast();
	}

	public T poll() {
		return pollFirst();
	}

	public T pollFirst() {
		if (size == 0)
			return null;
		else
			return removeFirst();
	}

	public T pollLast() {
		if (size == 0)
			return null;
		else
			return removeLast();
	}

	public T pop() {
		return removeFirst();
	}

	public void push(T e) {
		addFirst(e);
	}

	public T remove() {
		return removeFirst();
	}

	public T removeFirst() {
		if (size == 0)
			throw new NullPointerException("List is empty");
		else {
			myIterator.toStart();
			T result = myIterator.next();
			myIterator.remove();
			return result;
		}
	}

	public boolean removeFirstOccurrence(Object o) {
		myIterator.toStart();
		while (myIterator.hasNext()) {
			if (myIterator.next().equals(o)) {
				myIterator.remove();
				return true;
			}
		}
		return false;
	}

	public T removeLast() {
		if (size == 0)
			throw new NullPointerException("List is empty");
		else {
			myIterator.toEnd();
			T result = myIterator.previous();
			myIterator.remove();
			return result;
		}
	}

	public boolean removeLastOccurrence(Object o) {
		myIterator.toEnd();
		while (myIterator.hasPrevious()) {
			if (myIterator.previous().equals(o)) {
				myIterator.remove();
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		if (isEmpty())
			return "[]";
		StringBuilder sb = new StringBuilder("[");
		myIterator.toStart();
		sb.append(myIterator.next());
		while (myIterator.hasNext()) {
			sb.append(", ");
			sb.append(myIterator.next());
		}
		sb.append("]");
		return sb.toString();
	}
}
