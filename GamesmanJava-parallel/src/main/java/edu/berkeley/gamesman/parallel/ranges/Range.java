package edu.berkeley.gamesman.parallel.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;

import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;

public class Range<S extends GenState> implements WritableComparable<Range<S>> {
	private final IntArrWritable suffix = new IntArrWritable();

	public Range() {
	}

	public Range(int... values) {
		suffix.setLength(values.length);
		for (int i = 0; i < values.length; i++)
			suffix.set(i, values[i]);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		suffix.readFields(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		suffix.write(out);
	}

	@Override
	public int compareTo(Range<S> o) {
		return suffix.compareTo(o.suffix);
	}

	public void set(S t, int suffLen) {
		suffix.set(t, suffLen);
	}

	public int subHash(GenHasher<S> hasher, S t) {
		assert suffix.matches(t);
		long result = hasher.subHash(t, hasher.numElements - suffix.length());
		if (result > Integer.MAX_VALUE)
			throw new RuntimeException("subhash too large " + result);
		return (int) result;
	}

	public int length() {
		return suffix.length();
	}

	public int get(int i) {
		return suffix.get(i);
	}

	public boolean firstPosition(GenHasher<S> hasher, S toFill) {
		return suffix.<S> firstPosition(hasher, toFill);
	}

	@Override
	public String toString() {
		return suffix.toString();
	}

	public boolean matches(S position) {
		return suffix.matches(position);
	}

	@Override
	public int hashCode() {
		return suffix.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof Range)
				&& suffix.equals(((Range<?>) other).suffix);
	}
}
