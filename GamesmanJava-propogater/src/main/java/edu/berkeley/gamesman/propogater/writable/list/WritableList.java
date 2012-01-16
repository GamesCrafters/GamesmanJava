package edu.berkeley.gamesman.propogater.writable.list;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.propogater.factory.Factory;
import edu.berkeley.gamesman.propogater.factory.FactoryUtil;

public class WritableList<T extends Writable> implements Writable, WritList<T> {
	private final Factory<T> fact;
	private final ArrayList<T> arr = new ArrayList<T>();
	private int len = 0;

	private WritableList<T> appended = null;
	private boolean stolen = false;

	public WritableList(Class<? extends T> tClass, Configuration conf) {
		this(FactoryUtil.makeFactory(tClass, conf));
	}

	public WritableList(Factory<T> fact) {
		this.fact = fact;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		notModedCheck();
		clear();
		addFields(in);
	}

	private void notModedCheck() {
		if (stolen || appended != null)
			throw new RuntimeException("Cannot perform this while appended");
	}

	public void ensureCapacity(int len) {
		arr.ensureCapacity(len);
		for (int i = arr.size(); i < len; i++) {
			arr.add(fact.create());
		}
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(length());
		if (stolen)
			return;
		for (int i = 0; i < len; i++)
			arr.get(i).write(out);
		if (appended != null) {
			for (int i = 0; i < appended.len; i++)
				appended.arr.get(i).write(out);
		}
	}

	@Override
	public int length() {
		return stolen ? 0 : (len + (appended == null ? 0 : appended.len));
	}

	@Override
	public T get(int i) {
		if (stolen)
			throw new IndexOutOfBoundsException(i
					+ " is too large for array of length " + length());
		if (i < len)
			return arr.get(i);
		else {
			int j = i - len;
			if (appended != null && j < appended.len)
				return appended.arr.get(j);
			else
				throw new IndexOutOfBoundsException(i
						+ " is too large for array of length " + length());
		}
	}

	public void clear() {
		notModedCheck();
		len = 0;
	}

	public T add() {
		notModedCheck();
		ensureCapacity(len + 1);
		return arr.get(len++);
	}

	public void addFields(DataInput in) throws IOException {
		notModedCheck();
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
		notModedCheck();
		return arr.get(--len);
	}

	public boolean isEmpty() {
		return length() == 0;
	}

	public void magicSteal(WritableList<T> other) {
		notModedCheck();
		other.magicClear();
		appended = other;
	}

	public void revertSteal() {
		appended.revertClear();
		appended = null;
	}

	@Override
	public String toString() {
		if (stolen)
			return Collections.EMPTY_LIST.toString();
		List<T> subList1 = arr.subList(0, len);
		List<T> subList2 = (appended == null) ? Collections.<T> emptyList()
				: appended.arr.subList(0, appended.len);
		ArrayList<T> newList = new ArrayList<T>();
		newList.addAll(subList1);
		newList.addAll(subList2);
		return newList.toString();
	}

	public void magicClear() {
		notModedCheck();
		stolen = true;
	}

	public void revertClear() {
		stolen = false;
	}
}
