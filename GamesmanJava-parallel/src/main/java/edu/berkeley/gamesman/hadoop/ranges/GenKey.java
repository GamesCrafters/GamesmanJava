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
		tempNums = new int[myHasher.numElements];
	}

	private final GenHasher<S> myHasher;
	private final S myState;
	private transient final int[] tempNums;

	@Override
	public int compareTo(T o) {
		return myState.compareTo(o.myState);
	}

	public void get(GenHasher<S> hasher, S toFill) {
		hasher.set(toFill, myState);
	}

	public void getSuffix(int[] toFill, int length) {
		myState.getSuffix(toFill, length);
	}

	@Override
	public void set(T t) {
		myHasher.set(myState, t.myState);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		for (int i = myHasher.numElements - 1; i >= 0; i--) {
			tempNums[i] = in.readInt();
		}
		myHasher.set(myState, tempNums);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		for (int i = myHasher.numElements - 1; i >= 0; i--) {
			out.write(myState.get(i));
		}
	}

}
