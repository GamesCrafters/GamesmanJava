package edu.berkeley.gamesman.parallel.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.io.WritableComparable;

import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;

public class IntArrWritable implements WritableComparable<IntArrWritable> {
	private int[] arr = new int[0];
	private int arrLen;

	void setLength(int length) {
		arrLen = length;
		if (arr.length < arrLen)
			arr = new int[arrLen];
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		setLength(in.readInt());
		for (int i = 0; i < arrLen; i++)
			arr[i] = in.readInt();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(arrLen);
		for (int i = 0; i < arrLen; i++)
			out.writeInt(arr[i]);
	}

//	public void set(IntArrWritable t) {
//		setLength(t.arrLen);
//		System.arraycopy(t.arr, 0, arr, 0, arrLen);
//	}

	@Override
	public int compareTo(IntArrWritable o) {
		if (arrLen != o.arrLen)
			return arrLen - o.arrLen;
		for (int i = arrLen - 1; i >= 0; i--) {
			if (arr[i] != o.arr[i])
				return arr[i] - o.arr[i];
		}
		return 0;
	}

	public int length() {
		return arrLen;
	}

	public int get(int i) {
		assert i < arrLen;
		return arr[i];
	}

	public long numPositions(GenHasher<?> hasher) {
		return hasher.numPositions(arr, arrLen);
	}

	public <S extends GenState> boolean firstPosition(GenHasher<S> hasher,
			S toFill) {
		return hasher.firstPosition(arr, arrLen, toFill);
	}

	public boolean matches(GenState pos) {
		int startFrom = pos.numElements() - arrLen;
		for (int i = 0; i < arrLen; i++) {
			if (pos.get(i + startFrom) != arr[i])
				return false;
		}
		return true;
	}

	public void set(int i, int val) {
		assert i < arrLen;
		arr[i] = val;
	}

	public void set(GenState other, int suffLen) {
		setLength(suffLen);
		other.getSuffix(arr, suffLen);
	}

	@Override
	public int hashCode() {
		int hash = 1;
		for (int i = 0; i < arrLen; i++) {
			hash *= 31;
			hash += arr[i];
		}
		return hash;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof IntArrWritable))
			return false;
		IntArrWritable o = (IntArrWritable) other;
		if (arrLen != o.arrLen)
			return false;
		for (int i = 0; i < arrLen; i++) {
			if (arr[i] != o.arr[i])
				return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return Arrays.toString(Arrays.copyOfRange(arr, 0, arrLen));
	}
}
