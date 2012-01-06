package edu.berkeley.gamesman.propogater.writable.list;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;

import edu.berkeley.gamesman.propogater.factory.Factory;
import edu.berkeley.gamesman.propogater.factory.FactoryUtil;
import edu.berkeley.gamesman.propogater.writable.WritableSettable;


public final class WritableList<T extends WritableSettable<T>> implements
		WritableSettable<WritableList<? extends T>> {
	protected final ArrayList<T> myList = new ArrayList<T>(0);
	protected int length = 0;
	private final Factory<? extends T> fact;

	public WritableList(Factory<? extends T> fact) {
		this.fact = fact;
	}

	public WritableList(Class<? extends T> fClass, Configuration conf) {
		this(FactoryUtil.makeFactory(fClass, conf));
	}

	public void add(T obj) {
		checkContainsNext(length);
		myList.get(length).set(obj);
		length++;
	}

	public T add() {
		checkContainsNext(length);
		return myList.get(length++);
	}

	public void clear() {
		length = 0;
	}

	protected void checkInBounds(int i) {
		if (i >= length)
			throw new IndexOutOfBoundsException();
	}

	public T get(int i) {
		checkInBounds(i);
		return myList.get(i);
	}

	public int length() {
		return length;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(length);
		for (int i = 0; i < length; i++)
			myList.get(i).write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		length = in.readInt();
		myList.ensureCapacity(length);
		for (int i = 0; i < length; i++) {
			checkContainsNext(i);
			myList.get(i).readFields(in);
		}
	}

	private void checkContainsNext(int i) {
		if (myList.size() == i)
			myList.add(fact.create());
	}

	@Override
	public void set(WritableList<? extends T> other) {
		clear();
		addAll(other);
	}

	public void addAll(WritableList<? extends T> other) {
		ensureCapacity(length + other.length());
		for (int i = 0; i < other.length(); i++) {
			add(other.get(i));
		}
	}

	public void ensureCapacity(int length) {
		myList.ensureCapacity(length);
		while (myList.size() < length)
			myList.add(fact.create());
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof WritableList && equals((WritableList<?>) other);
	}

	public boolean equals(WritableList<?> other) {
		if (length != other.length)
			return false;
		for (int i = 0; i < length; i++) {
			if (!myList.get(i).equals(other.myList.get(i)))
				return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int code = 1;
		for (int i = 0; i < length; i++)
			code = code * 31 + myList.get(i).hashCode();
		return code;
	}

	@Override
	public String toString() {
		return myList.subList(0, length).toString();
	}

	public boolean isEmpty() {
		return length == 0;
	}
}
