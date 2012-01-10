package edu.berkeley.gamesman.hadoop.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.hasher.genhasher.Move;
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

	public void set(GenHasher<S> hasher, T t, int suffLen, Move[] moves) {
		setLength(suffLen);
		t.getSuffix(suffix, suffLen);
		addMoves(hasher, moves);
	}

	private void addMoves(GenHasher<S> hasher, Move[] moves) {
		moveList.clear();
		for (Move move : moves) {
			if (this.canMakeMove(hasher, move)) {
				MoveWritable writ = moveList.add();
				writ.set(move);
			}
		}
	}

	private boolean canMakeMove(GenHasher<S> h, Move move) {
		int startPoint = h.numElements - suffLen;
		for (int i = move.numChanges() - 1; i >= 0; i--) {
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

	public long firstPosition(GenHasher<S> hasher, int childNum, S toFill) {
		boolean exists = firstPosition(hasher, toFill);
		if (!exists)
			return -1;
		long total = 0L;
		while (!canMeet(hasher, childNum, toFill)) {
			long stepped = step(hasher, childNum, toFill);
			if (stepped == -1)
				return -1;
			else
				total += stepped;
		}
		return total;
	}

	public boolean firstPosition(GenHasher<S> hasher, S toFill) {
		return hasher.firstPosition(suffix, toFill);
	}

	private boolean canMeet(GenHasher<S> hasher, int childNum, S pos) {
		MoveWritable move = moveList.get(childNum);
		for (int i = 0; i < move.numChanges(); i++) {
			int place = move.getChangePlace(i);
			if (move.getChangeFrom(i) != pos.get(place))
				return false;
		}
		return true;
	}

	public long step(GenHasher<S> hasher, int childNum, S pos) {
		assert matches(pos);
		return hasher.stepTo(pos, moveList.get(childNum), hasher.numElements
				- suffLen);
	}

	public long subHash(GenHasher<S> hasher, S pos) {
		assert matches(pos);
		return hasher.subHash(pos, hasher.numElements - suffLen);
	}

	private boolean matches(S pos) {
		int startFrom = pos.numElements() - suffLen;
		for (int i = 0; i < suffLen; i++) {
			if (pos.get(i + startFrom) != suffix[i])
				return false;
		}
		return true;
	}

	public long indexOf(GenHasher<S> hasher, S pos, Move m) {
		S tempState = hasher.getPoolState();
		try {
			hasher.makeMove(pos, m, tempState);
			return hasher.subHash(pos, hasher.numElements - suffLen);
		} finally {
			hasher.release(tempState);
		}
	}

	public void makeMove(GenHasher<S> h, int moveNum, Move[] moves) {
		MoveWritable move = moveList.get(moveNum);
		int startPoint = h.numElements - suffLen;
		for (int i = move.numChanges() - 1; i >= 0; i--) {
			int place = move.getChangePlace(i);
			if (place < startPoint)
				break;
			if (move.getChangeFrom(i) != suffix[place - startPoint])
				throw new RuntimeException("Invalid move");
			else
				suffix[place - startPoint] = move.getChangeTo(i);
		}
		addMoves(h, moves);
	}

	public int numMoves() {
		return moveList.length();
	}

	public MoveWritable getMove(int childNum) {
		return moveList.get(childNum);
	}
}