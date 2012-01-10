package edu.berkeley.gamesman.hadoop.ranges;

import java.util.Collection;
import java.util.HashSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ReflectionUtils;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.hasher.genhasher.Move;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.writable.list.WritableArray;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public abstract class RangeTree<S extends GenState, T extends GenKey<S, T>>
		extends Tree<Range<S, T>, RangeRecords> {
	private Move[] moves;

	@Override
	public Collection<Range<S, T>> getRoots() {
		Collection<T> startingPositions = getStartingPositions();
		HashSet<Range<S, T>> containingRanges = new HashSet<Range<S, T>>();
		for (T t : startingPositions) {
			Range<S, T> containingRange = makeContainingRange(t);
			containingRanges.add(containingRange);
		}
		return containingRanges;
	}

	private Range<S, T> makeContainingRange(T t) {
		Range<S, T> range = newRange();
		makeContainingRange(t, range);
		return range;
	}

	private void makeContainingRange(T t, Range<S, T> range) {
		range.set(getHasher(), t, suffixLength(), moves);
	}

	protected abstract int suffixLength();

	private final Range<S, T> newRange() {
		return ReflectionUtils.newInstance(getKeyClass(), getConf());
	}

	protected abstract Collection<T> getStartingPositions();

	@Override
	public void getChildren(Range<S, T> position,
			WritableList<Range<S, T>> toFill) {
		for (int i = 0; i < position.numMoves(); i++) {
			Range<S, T> result = toFill.add();
			result.set(position);
			result.makeMove(getHasher(), i, moves);
		}
	}

	@Override
	public boolean getInitialValue(Range<S, T> position, RangeRecords toFill) {
		GenHasher<S> hasher = getHasher();
		long lPositions = position.numPositions(hasher);
		if (lPositions > Integer.MAX_VALUE)
			throw new RuntimeException("Too large for me");
		int numPositions = (int) lPositions;
		toFill.setLength(numPositions);
		boolean result = false;
		S tempState = hasher.getPoolState();
		try {
			position.firstPosition(hasher, tempState);
			for (int i = 0; i < numPositions; i++) {
				result |= setInitialRecord(tempState, toFill.setHasAndGet(i));
				hasher.step(tempState);
			}
		} finally {
			hasher.release(tempState);
		}
		return result;
	}

	private boolean setInitialRecord(S state, GameRecord rec) {
		GameValue val = getValue(state);
		if (val == null) {
			rec.set(GameValue.DRAW);
			return true;
		} else if (val.hasRemoteness) {
			rec.set(val, 0);
		} else
			rec.set(val);
		return false;
	}

	protected abstract GameValue getValue(S state);

	@Override
	public void travelUp(RangeRecords tVal, int childNum, Range<S, T> child,
			Range<S, T> parent, RangeRecords toFill) {
		GenHasher<S> hasher = getHasher();
		MoveWritable move = parent.getMove(childNum);
		long lParentPositions = parent.numPositions(hasher);
		assert lParentPositions <= Integer.MAX_VALUE;
		int numParentPositions = (int) lParentPositions;
		toFill.setLength(numParentPositions);
		S state = hasher.getPoolState();
		try {
			long lChange = parent.firstPosition(hasher, childNum, state);
			assert lChange <= Integer.MAX_VALUE;
			int change = (int) lChange;
			for (int i = change; change != -1; i += change) {
				assert child.subHash(hasher, state) == i;
				long lIndex = child.indexOf(hasher, state, move);
				assert lIndex <= Integer.MAX_VALUE;
				GameRecord childRec = tVal.get((int) lIndex);
				tempRecord.previousPosition(childRec);
				toFill.set(i, tempRecord);
				lChange = parent.step(hasher, childNum, state);
				assert i + lChange <= Integer.MAX_VALUE;
				change = (int) lChange;
			}
		} finally {
			hasher.release(state);
		}
	}

	@Override
	public Class<RangeRecords> getValClass() {
		return RangeRecords.class;
	}

	private final GameRecord tempRecord = new GameRecord();

	@Override
	public boolean combine(WritableArray<RangeRecords> children,
			RangeRecords toFill) {
		int numPositions = toFill.numPositions();
		assert allMatches(children, numPositions);
		boolean changed = false;
		for (int pos = 0; pos < numPositions; pos++) {
			boolean hasValue = false;
			for (int i = 0; i < children.length(); i++) {
				if (children.hasValue(i) && children.get(i).hasValue(pos)) {
					if (hasValue)
						tempRecord.combineWith(children.get(i).get(pos));
					else {
						hasValue = true;
						tempRecord.set(children.get(i).get(pos));
					}
				}
			}
			changed |= !tempRecord.equals(toFill.get(pos));
			toFill.set(pos, tempRecord);
		}
		return changed;
	}

	private boolean allMatches(WritableArray<RangeRecords> children,
			int numPositions) {
		for (int i = 0; i < children.length(); i++) {
			if (children.hasValue(i)
					&& children.get(i).numPositions() != numPositions)
				return false;
		}
		return true;
	}

	protected abstract int getVarianceLength();

	protected abstract GenHasher<S> getHasher();

	protected final void configure(Configuration conf) {
		innerConfigure(conf);
		moves = getMoves();
	}

	protected abstract Move[] getMoves();

	protected void innerConfigure(Configuration conf) {
	}
}