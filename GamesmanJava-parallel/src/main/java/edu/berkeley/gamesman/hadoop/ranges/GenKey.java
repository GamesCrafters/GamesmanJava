package edu.berkeley.gamesman.hadoop.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;

public class GenKey<S extends GenState, T extends GenKey<S, T>> implements
		WritableSettableComparable<T> {
	public GenKey(GenHasher<S> hasher) {
		myHasher = hasher;
		myState = hasher.newState();
	}

	private final GenHasher<S> myHasher;
	private final S myState;

	@Override
	public int compareTo(T o) {
		return myState.compareTo(o.myState);
	}

	public void get(GenHasher<S> hasher, S toFill) {
		hasher.set(toFill, myState);
	}

	@Override
	public void readFields(DataInput arg0) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void write(DataOutput arg0) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void set(T t) {
		// TODO Auto-generated method stub

	}

}
