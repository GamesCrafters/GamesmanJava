package edu.berkeley.gamesman.hadoop.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public abstract class Range<T extends GenKey<?, T>> implements
		WritableSettableComparable<Range<T>> {
	private final WritableList<Suffix> childrenList = new WritableList<Suffix>(
			Suffix.class, null);
	private final Suffix suff = new Suffix();

	@Override
	public void readFields(DataInput in) throws IOException {
		suff.readFields(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		suff.write(out);
	}

	@Override
	public void set(Range<T> t) {
		suff.set(t.suff);
	}

	@Override
	public int compareTo(Range<T> o) {
		return suff.compareTo(o.suff);
	}

	public abstract T newKey();

	public void setSuffix(T t, int suffLen) {
		suff.set(t, suffLen);
	}
}
