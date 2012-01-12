package edu.berkeley.gamesman.propogater.writable.list;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;

import edu.berkeley.gamesman.propogater.factory.Factory;
import edu.berkeley.gamesman.propogater.writable.IntEntry;
import edu.berkeley.gamesman.propogater.writable.WritableSettable;

public class WritableTreeMap<T extends WritableSettable<T>> implements
		WritableSettable<WritableTreeMap<T>> {
	private final WritableList<IntEntry<T>> objs;
	private int counter;

	public WritableTreeMap(Class<? extends T> fClass, Configuration conf) {
		objs = new WritableList<IntEntry<T>>(WritableTreeMap.<T> makeFact(
				fClass, conf));
	}

	public T getNext(int i) {
		while (counter < objs.length() && i > objs.get(counter).getInt())
			counter++;
		if (counter == objs.length() || i < objs.get(counter).getInt()) {
			if (counter > 0 && i <= objs.get(counter - 1).getInt())
				throw new RuntimeException("Counting backwards");
			else
				return null;
		} else {
			assert counter < objs.length();
			return objs.get(counter++).getKey();
		}
	}

	public void restart() {
		counter = 0;
	}

	public void clear() {
		objs.clear();
	}

	public int size() {
		return objs.length();
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
	public void set(WritableTreeMap<T> t) {
		objs.set(t.objs);
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
		if (!objs.isEmpty() && i <= objs.get(objs.length() - 1).getInt())
			throw new RuntimeException("Cannot add " + i
					+ ", must be greater than "
					+ objs.get(objs.length() - 1).getInt());
		IntEntry<T> entry = objs.add();
		entry.setInt(i);
		return entry.getKey();
	}

	private static <T extends WritableSettable<T>> Factory<IntEntry<T>> makeFact(
			final Class<? extends T> fClass, final Configuration conf) {
		return new Factory<IntEntry<T>>() {
			@Override
			public IntEntry<T> create() {
				return new IntEntry<T>(fClass, conf);
			}
		};
	}
}
