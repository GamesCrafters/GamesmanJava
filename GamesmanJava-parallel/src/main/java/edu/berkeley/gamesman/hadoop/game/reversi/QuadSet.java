package edu.berkeley.gamesman.hadoop.game.reversi;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.io.WritableComparator;

import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;


public class QuadSet implements WritableSettableComparable<QuadSet> {
	private byte[] bs = new byte[0];
	private int numEntries;
	private int bytesNeeded;

	public QuadSet(int numEntries) {
		setNumEntries(numEntries);
	}

	public void setNumEntries(int numEntries) {
		this.numEntries = numEntries;
		this.bytesNeeded = bytesNeeded(numEntries);
		if (bs.length < bytesNeeded) {
			bs = new byte[bytesNeeded];
		} else
			Arrays.fill(bs, (byte) 0);
	}

	private static int bytesNeeded(int numEntries) {
		return (numEntries + 3) / 4;
	}

	public QuadSet() {
		this(0);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(numEntries);
		out.write(bs, 0, bytesNeeded);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		setNumEntries(in.readInt());
		in.readFully(bs, 0, bytesNeeded);
	}

	@Override
	public void set(QuadSet t) {
		setNumEntries(t.numEntries);
		System.arraycopy(t.bs, 0, bs, 0, bytesNeeded);
	}

	@Override
	public int compareTo(QuadSet o) {
		return WritableComparator.compareBytes(bs, 0, bytesNeeded, o.bs, 0,
				o.bytesNeeded);
	}

	public int get(int i) {
		int ind = getInd(i);
		int shift = getShift(i);
		return (bs[ind] >>> shift) & 3;
	}

	public void set(int i, int b) {
		int ind = getInd(i);
		int shift = getShift(i);
		bs[ind] &= ~(3 << shift);
		bs[ind] |= b << shift;
	}

	private static int getShift(int i) {
		return (i & 3) << 1;
	}

	private static int getInd(int i) {
		return i >> 2;
	}

	@Override
	public int hashCode() {
		int hc = 1;
		for (int i = 0; i < bytesNeeded; i++) {
			hc *= 31;
			hc += bs[i];
		}
		return hc;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof QuadSet && compareTo((QuadSet) other) == 0;
	}

	public int numEntries() {
		return numEntries;
	}
}
