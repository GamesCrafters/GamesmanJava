package edu.berkeley.gamesman.parallel.writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QLLFactory;

public class WritableTreeMap<T extends Writable> implements Writable {
	private final JumpList keys;
	private final WritableQLL<T> objs;
	private int nextKey;
	private T nextVal;

	public WritableTreeMap(QLLFactory<IntWritable> facti,
			Pool<IntWritable> pooli, QLLFactory<T> fact, Pool<T> pool) {
		keys = new JumpList(facti, pooli);
		objs = new WritableQLL<T>(fact, pool);
	}

	public T getNext(int i) {
		while (nextKey >= 0 && i > nextKey) {
			next();
		}
		if (nextKey == -1 || i < nextKey) {
			return null;
		} else {
			T lastVal = nextVal;
			next();
			return lastVal;
		}
	}

	private void next() {
		nextKey = keys.next();
		nextVal = objs.next();
	}

	public void restart() {
		keys.restart();
		objs.restart();
		next();
	}

	public void clear() {
		keys.clear();
		objs.clear();
	}

	public int size() {
		assert keys.size() == objs.size();
		return keys.size();
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

	public T add(int i) {
		if (!objs.isEmpty() && i <= keys.getLast())
			throw new RuntimeException("Cannot add " + i
					+ ", must be greater than " + keys.getLast());
		keys.add(i);
		return objs.add();
	}

	public int peekNext() {
		return nextKey;
	}
}
