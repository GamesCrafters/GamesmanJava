package edu.berkeley.gamesman.propogater.writable.list;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;

import edu.berkeley.gamesman.propogater.factory.Factory;
import edu.berkeley.gamesman.propogater.writable.ValueWrapper;
import edu.berkeley.gamesman.propogater.writable.WritableSettable;

public class WritableArray<T extends WritableSettable<T>> implements
		WritableSettable<WritableArray<T>> {
	private final WritableList<ValueWrapper<T>> objs;

	public WritableArray(Class<? extends T> fClass, Configuration conf) {
		objs = new WritableList<ValueWrapper<T>>(WritableArray.<T> makeFact(
				fClass, conf));
	}

	public T get(int i) {
		return objs.get(i).get();
	}

	public void set(int i, T t) {
		objs.get(i).set(t);
	}

	public void clear(int i) {
		objs.get(i).clear();
	}

	public void clear() {
		objs.clear();
	}

	public void setLength(int newlength) {
		objs.clear();
		objs.ensureCapacity(newlength);
		for (int i = 0; i < newlength; i++) {
			objs.add().clear();
		}
	}

	public int length() {
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
	public void set(WritableArray<T> t) {
		objs.set(t.objs);
	}

	public void merge(WritableArray<T> other, boolean overwrite) {
		int i;
		int othLength = other.length();
		objs.ensureCapacity(othLength);
		int minLength = Math.min(length(), othLength);
		for (i = 0; i < minLength; i++) {
			T oth = other.get(i);
			if (oth != null && (overwrite || !objs.get(i).hasValue()))
				set(i, oth);
		}
		for (; i < othLength; i++) {
			objs.add().set(other.get(i));
		}
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof WritableArray
				&& equals((WritableArray<?>) other);
	}

	public boolean equals(WritableArray<?> other) {
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

	private static <T extends WritableSettable<T>> Factory<ValueWrapper<T>> makeFact(
			final Class<? extends T> fClass, final Configuration conf) {
		return new Factory<ValueWrapper<T>>() {
			@Override
			public ValueWrapper<T> create() {
				return new ValueWrapper<T>(fClass, conf);
			}
		};
	}

	public T setHasAndGet(int childNum) {
		return objs.get(childNum).setHasAndGet();
	}
}
