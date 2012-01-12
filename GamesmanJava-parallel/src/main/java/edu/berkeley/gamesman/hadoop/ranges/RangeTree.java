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
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

public abstract class RangeTree<S extends GenState> extends
		Tree<Range<S>, RangeRecords> {
	private Move[] moves;

	@Override
	public Collection<Range<S>> getRoots() {
		Collection<S> startingPositions = getStartingPositions();
		HashSet<Range<S>> containingRanges = new HashSet<Range<S>>();
		for (S t : startingPositions) {
			Range<S> containingRange = makeContainingRange(t);
			containingRanges.add(containingRange);
		}
		return containingRanges;
	}

	public Range<S> makeContainingRange(S t) {
		Range<S> range = newRange();
		makeContainingRange(t, range);
		return range;
	}

	private void makeContainingRange(S t, Range<S> range) {
		range.set(getHasher(), t, suffixLength(), moves);
	}

	protected abstract int suffixLength();

	private final Range<S> newRange() {
		return ReflectionUtils.newInstance(getKeyClass(), getConf());
	}

	public abstract Collection<S> getStartingPositions();

	@Override
	public void getChildren(Range<S> position, WritableList<Range<S>> toFill) {
		for (int i = 0; i < position.numMoves(); i++) {
			Range<S> result = toFill.add();
			result.set(position);
			result.makeMove(getHasher(), i, moves);
		}
	}

	@Override
	public boolean getInitialValue(Range<S> position, RangeRecords toFill) {
		toFill.clear(RangeRecords.ARRAY, false);
		GenHasher<S> hasher = getHasher();
		S s = hasher.getPoolState();
		try {
			if (position.firstPosition(hasher, s)) {
				position.addMoves(hasher, moves);
				return position.numMoves() > 0;
			} else
				return false;
		} finally {
			hasher.release(s);
		}
	}

	private void initialize(Range<S> position, RangeRecords toFill) {
		toFill.initialize();
		GenHasher<S> hasher = getHasher();
		long lPositions = position.numPositions(hasher);
		if (lPositions > Integer.MAX_VALUE)
			throw new RuntimeException("Too large for me");
		int numPositions = (int) lPositions;
		S tempState = hasher.getPoolState();
		try {
			position.firstPosition(hasher, tempState);
			for (int i = 0; i < numPositions; i++) {
				setInitialRecord(tempState, toFill.add());
				hasher.step(tempState);
			}
		} finally {
			hasher.release(tempState);
		}
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

	public abstract GameValue getValue(S state);

	@Override
	public void travelUp(RangeRecords tVal, int childNum, Range<S> child,
			Range<S> parent, RangeRecords toFill) {
		if (!tVal.initialized()) {
			initialize(child, tVal);
		}
		GenHasher<S> hasher = getHasher();
		MoveWritable move = parent.getMove(childNum);
		toFill.clear(RangeRecords.MAP, true);
		S state = hasher.getPoolState();
		try {
			long lChange = parent.firstPosition(hasher, childNum, state);
			if (lChange != -1) {
				assert parent.matches(state);
			}
			assert lChange <= Integer.MAX_VALUE;
			int change = (int) lChange;
			for (int i = change; change != -1; i += change) {
				assert parent.subHash(hasher, state) == i;
				long lIndex = child.indexOf(hasher, state, move);
				assert lIndex <= Integer.MAX_VALUE;
				GameRecord childRec = tVal.get((int) lIndex);
				toFill.add(i).previousPosition(childRec);
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

	private final QuickLinkedList<RangeRecords> current = new QuickLinkedList<RangeRecords>();
	private final QuickLinkedList<RangeRecords> waiting = new QuickLinkedList<RangeRecords>();

	@Override
	public boolean combine(Range<S> posRange,
			WritableArray<RangeRecords> children, RangeRecords toReplace) {
		if (!toReplace.initialized()) {
			initialize(posRange, toReplace);
		}
		int numPositions = toReplace.numPositions();
		boolean changed = false;
		current.clear();
		waiting.clear();
		for (int i = 0; i < children.length(); i++) {
			RangeRecords child = children.get(i);
			if (child != null) {
				child.restart();
				current.push(child);
			}
		}
		for (int pos = 0; pos < numPositions; pos++) {
			if (toReplace.get(pos).isPrimitive())
				continue;
			GameRecord bestRecord = null;
			while (!waiting.isEmpty() && waiting.peek().peekNext() <= pos) {
				current.push(waiting.pop());
			}
			QuickLinkedList<RangeRecords>.QLLIterator iter = current.iterator();
			try {
				while (iter.hasNext()) {
					RangeRecords child = iter.next();
					GameRecord value = child.getNext(pos);
					if (value == null) {
						iter.remove();
						int childNext = child.peekNext();
						if (childNext >= 0) {
							assert childNext > pos;
							QuickLinkedList<RangeRecords>.QLLIterator waitingIter = waiting
									.listIterator();
							try {
								while (waitingIter.hasNext()) {
									if (childNext <= waitingIter.next()
											.peekNext()) {
										waitingIter.previous();
										break;
									}
								}
								waitingIter.add(child);
							} finally {
								waiting.release(waitingIter);
							}
						}
					} else {
						if (bestRecord == null) {
							bestRecord = value;
						} else {
							bestRecord = bestRecord.compareTo(value) < 0 ? value
									: bestRecord;
						}
					}
				}
			} finally {
				current.release(iter);
			}
			if (bestRecord != null) {
				changed |= !bestRecord.equals(toReplace.get(pos));
				toReplace.set(pos, bestRecord);
			}
		}
		return changed;
	}

	protected abstract GenHasher<S> getHasher();

	protected final void configure(Configuration conf) {
		innerConfigure(conf);
		moves = getMoves();
	}

	protected abstract Move[] getMoves();

	protected void innerConfigure(Configuration conf) {
	}

	@Override
	public Class<? extends Range<S>> getKeyClass() {
		return (Class<? extends Range<S>>) Range.class
				.<Range> asSubclass(Range.class);
	}

	public int getDivision(Range<S> range) {
		GenHasher<S> hasher = getHasher();
		S tempState = hasher.getPoolState();
		try {
			range.firstPosition(hasher, tempState);
			return getDivision(tempState);
		} finally {
			hasher.release(tempState);
		}
	}

	protected int getDivision(S state) {
		return 0;
	}

	public GameRecord getRecord(Range<S> range, S state, RangeRecords records) {
		long iVal = range.subHash(getHasher(), state);
		assert iVal <= Integer.MAX_VALUE;
		return records.get((int) iVal);
	}
}
