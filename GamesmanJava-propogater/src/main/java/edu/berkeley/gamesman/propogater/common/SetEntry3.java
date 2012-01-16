package edu.berkeley.gamesman.propogater.common;

import java.util.Arrays;

import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.propogater.writable.Entry;


public class SetEntry3<T1 extends Writable, T2 extends Writable, T3> implements
		Entry3<T1, T2, T3> {
	private T1 t1;
	private T2 t2;
	private T3 t3;

	public SetEntry3() {
	}

	public SetEntry3(Entry<T1, T2> entry, T3 dm) {
		t1 = entry.getKey();
		t2 = entry.getValue();
		t3 = dm;
	}

	public void setEntries(T1 t1, T2 t2, T3 t3) {
		this.t1 = t1;
		this.t2 = t2;
		this.t3 = t3;
	}

	@Override
	public T1 getT1() {
		return t1;
	}

	@Override
	public T2 getT2() {
		return t2;
	}

	@Override
	public T3 getT3() {
		return t3;
	}

	@Override
	public String toString() {
		return Arrays.toString(new Object[] { t1, t2, t3 });
	}
}
