package edu.berkeley.gamesman.hadoop.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;

public abstract class Range<T extends GenKey<?, T>> implements
		WritableSettableComparable<Range<T>> {
	private final T firstElement, lastElement;

	public Range(T firstElement, T lastElement) {
		this.firstElement = firstElement;
		this.lastElement = lastElement;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		firstElement.readFields(in);
		lastElement.readFields(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		firstElement.write(out);
		lastElement.write(out);
	}

	@Override
	public void set(Range<T> t) {
		firstElement.set(t.firstElement);
		lastElement.set(t.lastElement);
	}

	@Override
	public int compareTo(Range<T> o) {
		return firstElement.compareTo(o.firstElement);
	}

	public abstract T newKey();
}
