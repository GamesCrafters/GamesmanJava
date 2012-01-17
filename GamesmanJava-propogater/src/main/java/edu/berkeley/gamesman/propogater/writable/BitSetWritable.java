package edu.berkeley.gamesman.propogater.writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.io.Writable;

public class BitSetWritable implements Writable {
	private byte[] bsBytes = new byte[0];
	private int numBytes;

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(numBytes);
		out.write(bsBytes, 0, numBytes);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		numBytes = in.readInt();
		ensureSize();
		in.readFully(bsBytes, 0, numBytes);
	}

	public void set(int i) {
		increaseLength(numBytes(i + 1));
		bsBytes[byteNum(i)] |= shiftNum(i);
	}

	private void increaseLength(int newLen) {
		int oldLen = numBytes;
		if (numBytes <= newLen)
			numBytes = newLen;
		ensureSize();
		for (int i = oldLen; i < newLen; i++)
			bsBytes[i] = 0;
	}

	public void clear(int i) {
		int byteNum = byteNum(i);
		if (byteNum < numBytes)
			bsBytes[byteNum] &= ~shiftNum(i);
	}

	public boolean get(int i) {
		int byteNum = byteNum(i);
		return byteNum < numBytes && ((bsBytes[byteNum] & shiftNum(i)) != 0);
	}

	private byte shiftNum(int i) {
		return (byte) (1 << (i & 7));
	}

	private int byteNum(int i) {
		return i >> 3;
	}

	private void ensureSize() {
		if (bsBytes.length < numBytes) {
			byte[] newBytes = new byte[numBytes];
			System.arraycopy(bsBytes, 0, newBytes, 0, bsBytes.length);
			bsBytes = newBytes;
		}
	}

	private static int numBytes(int numBits) {
		return (numBits + 7) >> 3;
	}

	public void clear() {
		Arrays.fill(bsBytes, 0, numBytes, (byte) 0);
	}

	public boolean isEmpty() {
		for (int i = 0; i < numBytes; i++)
			if (bsBytes[i] != 0)
				return false;
		return true;
	}
}
