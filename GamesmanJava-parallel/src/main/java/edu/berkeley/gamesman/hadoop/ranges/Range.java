package edu.berkeley.gamesman.hadoop.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.hasher.cachehasher.CacheMove;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;

public abstract class Range<T extends GenKey<?, T>> implements
		WritableSettableComparable<Range<T>> {
	private int[] suffix = new int[0];
	private int suffLen;

	private void setLength(int length) {
		suffLen = length;
		if (suffix.length < suffLen)
			suffix = new int[suffLen];
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		setLength(in.readInt());
		for (int i = 0; i < suffLen; i++)
			suffix[i] = in.readInt();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(suffLen);
		for (int i = 0; i < suffLen; i++)
			out.writeInt(suffix[i]);
	}

	@Override
	public void set(Range<T> t) {
		setLength(t.suffLen);
		System.arraycopy(t.suffix, 0, suffix, 0, suffLen);
	}

	@Override
	public int compareTo(Range<T> o) {
		if (suffLen != o.suffLen)
			return suffLen - o.suffLen;
		for (int i = suffLen - 1; i >= 0; i--) {
			if (suffix[i] != o.suffix[i])
				return suffix[i] - o.suffix[i];
		}
		return 0;
	}

	public abstract T newKey();

	public void set(T t, int suffLen, CacheMove[] moves) {
		setLength(suffLen);
		t.getSuffix(suffix, suffLen);
		// TODO Add in moves
	}

	public long numPositions(GenHasher<?> hasher) {
		return hasher.numPositions(suffix);
	}

	public <S extends GenState> boolean firstPosition(GenHasher<S> hasher,
			S toFill) {
		return hasher.firstPosition(suffix, toFill);
	}

	public void makeMove(int moveNum, CacheMove[] moves) {
		// TODO Auto-generated method stub

	}

	public int numMoves() {
		// TODO Auto-generated method stub
		return 0;
	}
}
