package edu.berkeley.gamesman.parallel.ranges;

import java.util.Collection;
import java.util.HashSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.util.ReflectionUtils;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.hasher.genhasher.Move;
import edu.berkeley.gamesman.hasher.genhasher.Moves;
import edu.berkeley.gamesman.propogater.common.Adder;
import edu.berkeley.gamesman.propogater.common.Entry3;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.writable.Entry;
import edu.berkeley.gamesman.propogater.writable.list.WritList;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QLLFactory;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

public abstract class RangeTree<S extends GenState> extends
		Tree<Range<S>, MainRecords, ChildMap, RecordMap, RecordMap, ChildMap> {

	private GenHasher<S> myHasher;
	private Move[] moves;
	private int varLen;
	private int suffLen;

	private QuickLinkedList<MutPair<Move, ChildMap>>[] movePlaces;
	private QuickLinkedList<MutPair<Move, ChildMap>> currentMoves;

	private final Pool<MutPair<Move, ChildMap>> movePool = new Pool<MutPair<Move, ChildMap>>(
			new Factory<MutPair<Move, ChildMap>>() {
				@Override
				public MutPair<Move, ChildMap> newObject() {
					return new MutPair<Move, ChildMap>();
				}

				@Override
				public void reset(MutPair<Move, ChildMap> t) {
				}
			});

	public abstract GameValue getValue(S state);

	@Override
	public void firstVisit(Range<S> key, MainRecords valueToFill,
			WritList<Entry<Range<S>, ChildMap>> parents,
			Adder<Entry3<Range<S>, RecordMap, ChildMap>> childrenToFill) {
		for (int i = 0; i < varLen; i++) {
			movePlaces[i].clear();
		}
		S state = myHasher.getPoolState();
		S childState = myHasher.getPoolState();
		try {
			if (key.firstPosition(myHasher, state)) {
				for (int i = 0; i < moves.length; i++) {
					MutPair<Move, ChildMap> pair = movePool.get();
					pair.car = moves[i];
					pair.cdr = null;
					currentMoves.add(pair);
				}
				int changed = 0, pos = 0;
				while (changed >= 0 && changed < varLen) {
					GameValue primitiveValue = getValue(state);
					if (primitiveValue == null) {
						valueToFill.add().set(GameRecord.DRAW);
						QuickLinkedList<MutPair<Move, ChildMap>>.QLLIterator iter = currentMoves
								.listIterator();
						try {
							while (iter.hasNext()) {
								MutPair<Move, ChildMap> movePair = iter.next();
								int place = Moves.matches(movePair.car, state);
								if (place == -1) {
									myHasher.makeMove(state, movePair.car,
											childState);
									if (movePair.cdr == null) {
										Entry3<Range<S>, RecordMap, ChildMap> toFill = childrenToFill
												.add();
										toFill.getT1().set(childState, suffLen);
										toFill.getT2().clear();
										ChildMap childMap = toFill.getT3();
										childMap.clear();
										movePair.cdr = childMap;
									}
									IntWritable writ = movePair.cdr.add(pos);
									long subHash = myHasher.subHash(childState,
											varLen);
									assert subHash <= Integer.MAX_VALUE;
									writ.set((int) subHash);
								} else if (place < varLen) {
									iter.remove();
									movePlaces[place].add(movePair);
								} else {
									iter.remove();
									movePool.release(movePair);
								}
							}
						} finally {
							currentMoves.release(iter);
						}
					} else {
						valueToFill.add().set(primitiveValue, 0);
					}
					changed = myHasher.step(state);
					pos++;
					for (int place = 0; place < varLen && place <= changed; place++) {
						currentMoves.stealAll(movePlaces[place]);
					}
				}
				while (!currentMoves.isEmpty())
					movePool.release(currentMoves.pop());
			}
		} finally {
			myHasher.release(childState);
			myHasher.release(state);
		}
	}

	@Override
	public void combineDown(Range<S> key, MainRecords value,
			WritList<Entry<Range<S>, ChildMap>> parents, int firstNewParent,
			WritableList<Entry<Range<S>, RecordMap>> children) {
	}

	private final QuickLinkedList<RecordMap> current = new QuickLinkedList<RecordMap>();
	private final QuickLinkedList<RecordMap> waiting = new QuickLinkedList<RecordMap>();

	@Override
	public boolean combineUp(Range<S> key, MainRecords value,
			WritList<Entry<Range<S>, ChildMap>> parents,
			WritableList<Entry<Range<S>, RecordMap>> children) {
		int numPositions = value.length();
		boolean changed = false;
		current.clear();
		waiting.clear();
		for (int i = 0; i < children.length(); i++) {
			RecordMap child = children.get(i).getValue();
			if (child != null) {
				child.restart();
				current.push(child);
			}
		}
		for (int pos = 0; pos < numPositions; pos++) {
			if (value.get(pos).isPrimitive())
				continue;
			GameRecord bestRecord = null;
			while (!waiting.isEmpty() && waiting.peek().peekNext() <= pos) {
				current.push(waiting.pop());
			}
			QuickLinkedList<RecordMap>.QLLIterator iter = current.iterator();
			try {
				while (iter.hasNext()) {
					RecordMap child = iter.next();
					GameRecord gvalue = child.getNext(pos);
					if (gvalue == null) {
						iter.remove();
						int childNext = child.peekNext();
						if (childNext >= 0) {
							assert childNext > pos;
							QuickLinkedList<RecordMap>.QLLIterator waitingIter = waiting
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
							bestRecord = gvalue;
						} else {
							bestRecord = bestRecord.compareTo(gvalue) < 0 ? gvalue
									: bestRecord;
						}
					}
				}
			} finally {
				current.release(iter);
			}
			if (bestRecord != null) {
				changed |= !bestRecord.equals(value.get(pos));
				value.get(pos).set(bestRecord);
			}
		}
		return changed;
	}

	@Override
	public void receiveDown(Range<S> key, MainRecords currentValue,
			Range<S> parentKey, ChildMap parentMessage, ChildMap toFill) {
		toFill.set(parentMessage);
	}

	@Override
	public void receiveUp(Range<S> key, MainRecords currentValue,
			Range<S> childKey, RecordMap childMessage,
			RecordMap currentChildInfo) {
		currentChildInfo.set(childMessage);
	}

	@Override
	public void sendUp(Range<S> key, MainRecords value, Range<S> parentKey,
			ChildMap parentInfo, RecordMap toFill) {
		toFill.clear();
		parentInfo.restart();
		int nextParentNum = parentInfo.peekNext();
		while (nextParentNum >= 0) {
			int nextChildNum = parentInfo.getNext(nextParentNum).get();
			toFill.add(nextParentNum).previousPosition(value.get(nextChildNum));
			nextParentNum = parentInfo.peekNext();
		}
	}

	@Override
	public Class<Range<S>> getKeyClass() {
		return (Class<Range<S>>) Range.class.<Range> asSubclass(Range.class);
	}

	@Override
	public Class<MainRecords> getValClass() {
		return MainRecords.class;
	}

	@Override
	public Class<ChildMap> getPiClass() {
		return ChildMap.class;
	}

	@Override
	public Class<RecordMap> getCiClass() {
		return RecordMap.class;
	}

	@Override
	public Class<RecordMap> getUmClass() {
		return RecordMap.class;
	}

	@Override
	public Class<ChildMap> getDmClass() {
		return ChildMap.class;
	}

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
		range.set(t, suffLen);
	}

	private final Range<S> newRange() {
		return ReflectionUtils.newInstance(getKeyClass(), getConf());
	}

	public abstract Collection<S> getStartingPositions();

	@Override
	public final void configure(Configuration conf) {
		innerConfigure(conf);
		myHasher = getHasher();
		moves = getMoves();
		suffLen = suffixLength();
		varLen = myHasher.numElements - suffLen;
		QLLFactory<MutPair<Move, ChildMap>> fact = new QLLFactory<MutPair<Move, ChildMap>>();
		currentMoves = fact.getList();
		movePlaces = new QuickLinkedList[varLen];
		for (int i = 0; i < varLen; i++) {
			movePlaces[i] = fact.getList();
		}
	}

	protected void innerConfigure(Configuration conf) {
	}

	protected abstract GenHasher<S> getHasher();

	protected abstract Move[] getMoves();

	protected abstract int suffixLength();

	public GameRecord getRecord(Range<S> range, S state, MainRecords records) {
		long iVal = range.subHash(getHasher(), state);
		assert iVal <= Integer.MAX_VALUE;
		return records.get((int) iVal);
	}
}
