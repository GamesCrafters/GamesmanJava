package edu.berkeley.gamesman.parallel.writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QLLFactory;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

public class WritableQLL<T extends Writable> implements Writable {
	private final Pool<T> myPool;
	private final QuickLinkedList<T> myList;
	private final QuickLinkedList<T>.QLLIterator myIter;

	WritableQLL(QLLFactory<T> fact, Pool<T> pool) {
		myPool = pool;
		myList = fact.getList();
		myIter = myList.iterator();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(myList.size());
		QuickLinkedList<T>.QLLIterator iter = myList.listIterator();
		try {
			while (iter.hasNext()) {
				iter.next().write(out);
			}
		} finally {
			myList.release(iter);
		}
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		int size = in.readInt();
		clear();
		for (int i = 0; i < size; i++) {
			add().readFields(in);
		}
	}

	public void clear() {
		QuickLinkedList<T>.QLLIterator iter = myList.listIterator();
		try {
			while (iter.hasNext()) {
				myPool.release(iter.next());
			}
		} finally {
			myList.release(iter);
		}
		myList.clear();
	}

	public T add() {
		T t = myPool.get();
		myList.add(t);
		return t;
	}

	public void restart() {
		myIter.toIndex(0);
	}

	public T next() {
		if (myIter.hasNext())
			return myIter.next();
		else
			return null;
	}

	public int size() {
		return myList.size();
	}

	public boolean isEmpty() {
		return myList.isEmpty();
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof WritableQLL)
				&& this.<Writable> equals((WritableQLL) other);
	}

	public <S extends Writable> boolean equals(WritableQLL<S> other) {
		QuickLinkedList<T>.QLLIterator myIter = myList.iterator();
		QuickLinkedList<S>.QLLIterator oIter = other.myList.iterator();
		try {
			while (myIter.hasNext()) {
				if (!oIter.hasNext())
					return false;
				T next = myIter.next();
				Object oNext = oIter.next();
				if (!next.equals(oNext))
					return false;
			}
			return !oIter.hasNext();
		} finally {
			other.myList.release(oIter);
			myList.release(myIter);
		}
	}

	public T getLast() {
		return myList.getLast();
	}

	@Override
	public String toString() {
		return myList.toString();
	}

}
