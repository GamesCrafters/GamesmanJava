package edu.berkeley.gamesman.propogater.common;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;

public class RawByteComparator extends WritableComparator {
	public static final RawByteComparator instance = new RawByteComparator();

	private RawByteComparator() {
		super(WritableComparable.class);
	}

	@Override
	public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
		return compareBytes(b1, s1, l1, b2, s2, l2);
	}
}
