package edu.berkeley.gamesman.hadoop.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;

public class GenKey<S extends GenState> implements
		WritableSettableComparable<GenKey<S>> {
	private final IntArrWritable myState = new IntArrWritable();

	@Override
	public int compareTo(GenKey<S> o) {
		return myState.compareTo(o.myState);
	}

	@Override
	public void set(GenKey<S> t) {
		myState.set(t.myState);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		myState.readFields(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		myState.write(out);
	}

	public IntArrWritable get() {
		return myState;
	}

}
