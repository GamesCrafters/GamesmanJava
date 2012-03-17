package edu.berkeley.gamesman.parallel.writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.propogater.writable.FixedLengthWritable;
import edu.berkeley.gamesman.propogater.writable.list.FLWritList;

public class WritableTreeMap<T extends FixedLengthWritable> implements Writable {
	private final JumpList keys;
	private final FLWritList<T> objs;
	private int nextKey;
	private int valPlace;

	public WritableTreeMap(T t) {
		keys = new JumpList();
		objs = new FLWritList<T>(t);
	}

	public T getNext(int i) {
		while (nextKey >= 0 && i > nextKey) {
			next();
		}
		if (nextKey == -1 || i < nextKey) {
			return null;
		} else {
			T lastVal = objs.get(valPlace);
			next();
			return lastVal;
		}
	}

	private void next() {
		nextKey = keys.next();
		valPlace++;
	}

	public void restart() {
		keys.restart();
		valPlace = 0;
		nextKey = keys.next();
	}

	public void clear(boolean adding) {
		keys.reset(adding);
		objs.reset(adding);
	}

	public int size() {
		return objs.length();
	}

	public boolean isEmpty() {
		assert keys.isEmpty() == objs.isEmpty();
		return keys.isEmpty();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		keys.write(out);
		objs.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		keys.readFields(in);
		objs.readFields(in);
		restart();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof WritableTreeMap
				&& equals((WritableTreeMap<?>) other);
	}

	public boolean equals(WritableTreeMap<?> other) {
		return keys.equals(other.keys) && objs.equals(other.objs);
	}

	@Override
	public int hashCode() {
		return keys.hashCode() * 31 + objs.hashCode();
	}

	@Override
	public String toString() {
		return keys.toString() + " : " + objs.toString();
	}

	public void add(int i, T t) {
		if (!objs.isEmpty() && i <= keys.getLast())
			throw new RuntimeException("Cannot add " + i
					+ ", must be greater than " + keys.getLast());
		keys.add(i);
		objs.add(t);
	}

	public int peekNext() {
		return nextKey;
	}

	public void finish() {
		keys.finish();
	}
}
