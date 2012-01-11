package edu.berkeley.gamesman.hadoop.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.hasher.genhasher.Move;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public class Range<S extends GenState> implements
		WritableSettableComparable<Range<S>> {
	private final WritableList<MoveWritable> moveList = new WritableList<MoveWritable>(
			MoveWritable.class, null);
	private final IntArrWritable suffix = new IntArrWritable();

	@Override
	public void readFields(DataInput in) throws IOException {
		suffix.readFields(in);
		moveList.readFields(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		suffix.write(out);
		moveList.write(out);
	}

	@Override
	public void set(Range<S> t) {
		suffix.set(t.suffix);
		moveList.set(t.moveList);
	}

	@Override
	public int compareTo(Range<S> o) {
		return suffix.compareTo(o.suffix);
	}

	public void set(GenHasher<S> hasher, GenKey<S> t, int suffLen, Move[] moves) {
		suffix.set(t.get(), suffLen);
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
		int startPoint = h.numElements - suffix.length();
		for (int i = move.numChanges() - 1; i >= 0; i--) {
			int place = move.getChangePlace(i);
			if (place < startPoint)
				return true;
			if (move.getChangeFrom(i) != suffix.get(place - startPoint))
				return false;
		}
		return true;
	}

	public long numPositions(GenHasher<?> hasher) {
		return suffix.numPositions(hasher);
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
		return suffix.firstPosition(hasher, toFill);
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
				- suffix.length());
	}

	public long subHash(GenHasher<S> hasher, S pos) {
		assert matches(pos);
		return hasher.subHash(pos, hasher.numElements - suffix.length());
	}

	private boolean matches(S pos) {
		return suffix.matches(pos);
	}

	public long indexOf(GenHasher<S> hasher, S pos, Move m) {
		S tempState = hasher.getPoolState();
		try {
			hasher.makeMove(pos, m, tempState);
			return hasher.subHash(pos, hasher.numElements - suffix.length());
		} finally {
			hasher.release(tempState);
		}
	}

	public void makeMove(GenHasher<S> h, int moveNum, Move[] moves) {
		MoveWritable move = moveList.get(moveNum);
		int startPoint = h.numElements - suffix.length();
		for (int i = move.numChanges() - 1; i >= 0; i--) {
			int place = move.getChangePlace(i);
			if (place < startPoint)
				break;
			if (move.getChangeFrom(i) != suffix.get(place - startPoint))
				throw new RuntimeException("Invalid move");
			else
				suffix.set(place - startPoint, move.getChangeTo(i));
		}
		addMoves(h, moves);
	}

	public int numMoves() {
		return moveList.length();
	}

	public MoveWritable getMove(int childNum) {
		return moveList.get(childNum);
	}

	@Override
	public int hashCode() {
		return suffix.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Range
				&& suffix.equals(((Range<?>) other).suffix);
	}

	@Override
	public String toString() {
		return suffix.toString();
	}

	public int get(int i) {
		return suffix.get(i);
	}

	public int length() {
		return suffix.length();
	}
}
