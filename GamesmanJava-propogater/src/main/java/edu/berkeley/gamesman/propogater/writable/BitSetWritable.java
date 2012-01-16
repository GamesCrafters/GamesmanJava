package edu.berkeley.gamesman.propogater.writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.io.Writable;

public class BitSetWritable implements Writable {
	private byte[] bsBytes = new byte[0];
	private int numBits;

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(numBits);
		out.write(bsBytes, 0, numBytes());
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		numBits = in.readInt();
		ensureSize();
		in.readFully(bsBytes, 0, numBytes());
	}

	public void set(int i) {
		increaseLength(i + 1);
		bsBytes[byteNum(i)] |= shiftNum(i);
	}

	private void increaseLength(int newLen) {
		int oldLen = numBits;
		if (numBits <= newLen)
			numBits = newLen;
		ensureSize();
		for (int i = oldLen; i < newLen; i++)
			clear(i);
	}

	public void clear(int i) {
		if (i < numBits)
			bsBytes[byteNum(i)] &= ~shiftNum(i);
	}

	public boolean get(int i) {
		return i < numBits ? ((bsBytes[byteNum(i)] & shiftNum(i)) != 0) : false;
	}

	private byte shiftNum(int i) {
		return (byte) (1 << (i & 7));
	}

	private int byteNum(int i) {
		return i >> 3;
	}

	private void ensureSize() {
		int numBytes = numBytes();
		if (bsBytes.length < numBytes)
			bsBytes = new byte[numBytes];
	}

	private int numBytes() {
		return numBytes(numBits);
	}

	private static int numBytes(int numBits) {
		return (numBits + 7) >> 3;
	}

	public void clear() {
		Arrays.fill(bsBytes, 0, numBytes(), (byte) 0);
	}
}
