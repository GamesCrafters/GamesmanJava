package edu.berkeley.gamesman.hadoop.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.hasher.cachehasher.CacheMove;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public abstract class Range<S extends GenState, T extends GenKey<S, T>>
		implements WritableSettableComparable<Range<S, T>> {
	private final WritableList<MoveWritable> moveList = new WritableList<MoveWritable>(
			MoveWritable.class, null);
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
		moveList.readFields(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(suffLen);
		for (int i = 0; i < suffLen; i++)
			out.writeInt(suffix[i]);
		moveList.write(out);
	}

	@Override
	public void set(Range<S, T> t) {
		setLength(t.suffLen);
		System.arraycopy(t.suffix, 0, suffix, 0, suffLen);
		moveList.set(t.moveList);
	}

	@Override
	public int compareTo(Range<S, T> o) {
		if (suffLen != o.suffLen)
			return suffLen - o.suffLen;
		for (int i = suffLen - 1; i >= 0; i--) {
			if (suffix[i] != o.suffix[i])
				return suffix[i] - o.suffix[i];
		}
		return 0;
	}

	public abstract T newKey();

	public void set(GenHasher<S> hasher, T t, int suffLen, CacheMove[] moves) {
		setLength(suffLen);
		t.getSuffix(suffix, suffLen);
		moveList.clear();
		for (CacheMove move : moves) {
			if (this.canMakeMove(hasher, move)) {
				MoveWritable writ = moveList.add();
				writ.set(move);
			}
		}
	}

	private boolean canMakeMove(GenHasher<S> h, CacheMove move) {
		int startPoint = h.numElements - suffLen;
		for (int i = move.numChanges - 1; i >= 0; i--) {
			int place = move.getChangePlace(i);
			if (place < startPoint)
				return true;
			if (move.getChangeFrom(i) != suffix[place - startPoint])
				return false;
		}
		return true;
	}

	public long numPositions(GenHasher<?> hasher) {
		return hasher.numPositions(suffix);
	}

	public void firstPosition(GenHasher<S> hasher, S toFill) {
		boolean exists = hasher.firstPosition(suffix, toFill);
		assert exists;
	}

	public void makeMove(int moveNum, CacheMove[] moves) {
		// TODO Auto-generated method stub

	}

	public int numMoves() {
		return moveList.length();
	}
}
