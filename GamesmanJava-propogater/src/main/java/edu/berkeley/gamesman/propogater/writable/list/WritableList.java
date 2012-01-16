package edu.berkeley.gamesman.propogater.writable.list;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.propogater.factory.Factory;
import edu.berkeley.gamesman.propogater.factory.FactoryUtil;

public class WritableList<T extends Writable> implements Writable, WritList<T> {
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
		clear();
		addFields(in);
	}

	private void ensureCapacity(int len) {
		arr.ensureCapacity(len);
		for (int i = arr.size(); i < len; i++) {
			arr.add(fact.create());
		}
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(len);
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
					+ " is too large for array of length " + len);
	}

	public void clear() {
		len = 0;
	}

	public T add() {
		ensureCapacity(len + 1);
		return arr.get(len++);
	}

	public void addFields(DataInput in) throws IOException {
		int addLen = in.readInt();
		int newLen = len + addLen;
		ensureCapacity(newLen);
		for (int i = len; i < newLen; i++)
			arr.get(i).readFields(in);
		len = newLen;
	}

	// Warning! Not really popped, do not try to add to list while still
	// accessing popped item
	public T popLast() {
		return arr.get(--len);
	}

	public boolean isEmpty() {
		return len == 0;
	}

	@Override
	public String toString() {
		return arr.subList(0, len).toString();
	}
}
