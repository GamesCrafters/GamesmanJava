package edu.berkeley.gamesman.propogater.writable.list;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.propogater.factory.Factory;
import edu.berkeley.gamesman.propogater.factory.FactoryUtil;
import edu.berkeley.gamesman.propogater.writable.Resetable;
import edu.berkeley.gamesman.propogater.writable.Resetables;

public class WritableList<T extends Writable> implements Writable, Resetable,
		WritList<T> {
	private final Factory<T> fact;
	private final ArrayList<T> arr = new ArrayList<T>();
	private int len = 0;

	public WritableList(Class<? extends T> tClass, Configuration conf) {
		this(FactoryUtil.makeFactory(tClass, conf));
	}

	public WritableList(Factory<T> fact) {
		this.fact = fact;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		int newLen = in.readInt();
		for (int i = newLen; i < len; i++) {
			Resetables.reset(arr.get(i));
		}
		len = newLen;
		ensureCapacity(len);
		for (int i = 0; i < len; i++) {
			arr.get(i).readFields(in);
		}
	}

	private void ensureCapacity(int len) {
		arr.ensureCapacity(len);
		for (int i = arr.size(); i < len; i++) {
			arr.add(fact.create());
			assert Resetables.checkReset(arr.get(arr.size() - 1));
		}
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(length());
		for (int i = 0; i < len; i++)
			arr.get(i).write(out);
	}

	@Override
	public int length() {
		return len;
	}

	@Override
	public T get(int i) {
		if (i < len)
			return arr.get(i);
		else
			throw new IndexOutOfBoundsException(i
					+ " is too large for array of length " + length());
	}

	public void clear() {
		for (int i = 0; i < len; i++) {
			Resetables.reset(arr.get(i));
		}
		len = 0;
	}

	public T add() {
		ensureCapacity(len + 1);
		assert Resetables.checkReset(arr.get(len));
		return arr.get(len++);
	}

	public boolean isEmpty() {
		return length() == 0;
	}

	public void steal(WritableList<T> other) {
		ensureCapacity(len + other.len);
		for (int i = 0; i < other.len; i++) {
			WritableList.<T> swap(arr, len + i, other.arr, i);
			assert Resetables.checkReset(other.arr.get(i));
		}
		len += other.len;
		other.len = 0;
	}

	private static <T> void swap(ArrayList<T> arr1, int i, ArrayList<T> arr2,
			int j) {
		arr1.set(i, arr2.set(j, arr1.get(i)));
	}

	@Override
	public String toString() {
		return arr.subList(0, len).toString();
	}

	@Override
	public void reset() {
		clear();
	}

	@Override
	public boolean checkReset() {
		return isEmpty();
	}
}
