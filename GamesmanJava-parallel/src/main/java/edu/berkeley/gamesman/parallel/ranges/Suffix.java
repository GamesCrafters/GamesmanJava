package edu.berkeley.gamesman.parallel.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;

import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;

public class Suffix<S extends GenState> implements
		WritableComparable<Suffix<S>> {
	private final IntArrWritable suffix = new IntArrWritable();

	public Suffix() {
	}

	public Suffix(int... values) {
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
	public int compareTo(Suffix<S> o) {
		return suffix.compareTo(o.suffix);
	}

	public void set(S t, int suffLen) {
		suffix.set(t, suffLen);
	}

	public int subHash(GenHasher<S> hasher, S t) {
		assert suffix.matches(t);
		long result = hasher
				.hash(t, null, hasher.numElements - suffix.length());
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

	public long numPositions(GenHasher<S> hasher) {
		return suffix.numPositions(hasher);
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
		return (other instanceof Suffix)
				&& suffix.equals(((Suffix<?>) other).suffix);
	}
}
