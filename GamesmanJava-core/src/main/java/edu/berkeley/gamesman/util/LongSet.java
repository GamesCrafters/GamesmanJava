package edu.berkeley.gamesman.util;

public class LongSet {
	public long value;

	public LongSet(long v) {
		value = v;
	}

	public LongSet() {
	}

	@Override
	public int hashCode() {
		return (int) (value ^ (value >>> 32));
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof LongSet && value == ((LongSet) other).value;
	}

	@Override
	public String toString() {
		return Long.toString(value);
	}
}
