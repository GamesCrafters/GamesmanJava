package edu.berkeley.gamesman.parallel.writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.propogater.writable.IntEntry;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QLLFactory;

public class WritableTreeMap<T extends Writable> implements Writable {
	private final WritableQLL<IntEntry<T>> objs;
	private IntEntry<T> next;

	public WritableTreeMap(QLLFactory<IntEntry<T>> fact, Pool<IntEntry<T>> pool) {
		objs = new WritableQLL<IntEntry<T>>(fact, pool);
	}

	public T getNext(int i) {
		while (next != null && i > next.getKey())
			next = objs.next();
		if (next == null || i < next.getKey()) {
			return null;
		} else {
			IntEntry<T> last = next;
			next = objs.next();
			return last.getValue();
		}
	}

	public void restart() {
		objs.restart();
		next = objs.next();
	}

	public void clear() {
		objs.clear();
	}

	public int size() {
		return objs.size();
	}

	public boolean isEmpty() {
		return objs.isEmpty();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		objs.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		objs.readFields(in);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof WritableTreeMap
				&& equals((WritableTreeMap<?>) other);
	}

	public boolean equals(WritableTreeMap<?> other) {
		return objs.equals(other.objs);
	}

	@Override
	public int hashCode() {
		return objs.hashCode();
	}

	@Override
	public String toString() {
		return objs.toString();
	}

	public T add(int i) {
		if (!objs.isEmpty() && i <= objs.getLast().getKey())
			throw new RuntimeException("Cannot add " + i
					+ ", must be greater than " + objs.getLast().getKey());
		IntEntry<T> entry = objs.add();
		entry.setKey(i);
		return entry.getValue();
	}

	public int peekNext() {
		if (next == null)
			return -1;
		else
			return next.getKey();
	}
}
