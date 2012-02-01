package edu.berkeley.gamesman.parallel.ranges;

import java.util.Arrays;
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
import edu.berkeley.gamesman.propogater.tree.SimpleTree;
import edu.berkeley.gamesman.propogater.writable.Entry;
import edu.berkeley.gamesman.propogater.writable.list.WritList;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QLLFactory;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

public abstract class RangeTree<S extends GenState> extends
		SimpleTree<Range<S>, MainRecords, ChildMap, RecordMap> {

	private class SaveMove {
		public SaveMove() {
			childState = myHasher.newState();
			rotatedState = myHasher.newState();
			childSubHashes = new long[myHasher.numElements];
		}

		final S childState;
		final S rotatedState;
		final long[] childSubHashes;
		Move move;
		ChildMap map;
		int lastParentPosition;
	}

	private GenHasher<S> myHasher;
	private Move[] moves;
	private int varLen;
	private int suffLen;
	private int[] rotDep;

	// Temporary variables
	private QuickLinkedList<SaveMove>[] movePlaces;
	private QuickLinkedList<SaveMove> currentMoves;
	private int[] lastChanged;
	private S state;

	private final Pool<SaveMove> movePool = new Pool<SaveMove>(
			new Factory<SaveMove>() {
				@Override
				public SaveMove newObject() {
					return new SaveMove();
				}

				@Override
				public void reset(SaveMove t) {
				}
			});

	public abstract GameValue getValue(S state);

	@Override
	public void firstVisit(Range<S> key, MainRecords valueToFill,
			WritList<Entry<Range<S>, ChildMap>> parents,
			Adder<Entry3<Range<S>, RecordMap, ChildMap>> childrenToFill) {
		boolean hasFirst = key.firstPosition(myHasher, state);
		assert hasFirst;
		valueToFill.clear();
		Arrays.fill(lastChanged, 0);
		for (Move move : moves) {
			SaveMove sm = movePool.get();
			sm.move = move;
			sm.lastParentPosition = -1;
			Arrays.fill(sm.childSubHashes, 0L);
			currentMoves.add(sm);
		}
		int changed = myHasher.numElements - 1;
		int pos = 0;
		while (true) {
			assert pos == myHasher.hash(state, null, varLen);
			GameValue prim = getValue(state);
			GameRecord rec = valueToFill.add();
			if (prim == null) {
				rec.set(GameValue.DRAW);
				QuickLinkedList<SaveMove>.QLLIterator sIter = currentMoves
						.iterator();
				try {
					while (sIter.hasNext()) {
						SaveMove sm = sIter.next();
						int matches = Moves.matches(sm.move, state);
						if (matches == -1) {
							if (validMove(state, sm.move)) {
								int numChanged;
								if (sm.map == null) {
									numChanged = varLen;
									myHasher.makeMove(state, sm.move,
											sm.childState, myHasher.numElements);
									rotateToBaseState(myHasher, sm.childState,
											sm.rotatedState,
											myHasher.numElements);
									Entry3<Range<S>, RecordMap, ChildMap> entry = childrenToFill
											.add();
									entry.getT1().set(sm.rotatedState, suffLen);
									entry.getT2().clear();
									ChildMap childMap = entry.getT3();
									childMap.clear();
									sm.map = childMap;
								} else {
									numChanged = 0;
									while (numChanged < varLen
											&& sm.lastParentPosition < lastChanged[numChanged])
										numChanged++;
									myHasher.makeMove(state, sm.move,
											sm.childState, numChanged);
									rotateToBaseState(myHasher, sm.childState,
											sm.rotatedState, numChanged);
								}
								long childHash = myHasher.hash(sm.rotatedState,
										sm.childSubHashes, rotDep[numChanged]);
								assert childHash == myHasher.hash(
										sm.rotatedState, null, varLen);
								assert childHash <= Integer.MAX_VALUE;
								IntWritable writ = sm.map.add(pos);
								writ.set((int) childHash);
								sm.lastParentPosition = pos;
							}
						} else {
							sIter.remove();
							if (matches < varLen) {
								movePlaces[matches].add(sm);
							} else {
								movePool.release(sm);
							}
						}
					}
				} finally {
					currentMoves.release(sIter);
				}
			} else {
				rec.set(prim, 0);
			}
			changed = myHasher.step(state);
			pos++;
			for (int i = Math.min(changed, varLen - 1); i >= 0; i--) {
				currentMoves.stealAll(movePlaces[i]);
			}
			if (changed < 0 || changed >= varLen)
				break;
			for (int i = rotDep[changed]; i >= 0; i--) {
				lastChanged[i] = pos;
			}
		}
		QuickLinkedList<SaveMove>.QLLIterator iter = currentMoves.iterator();
		while (iter.hasNext())
			movePool.release(iter.next());
		currentMoves.release(iter);
		currentMoves.clear();
	}

	public boolean validMove(S state, Move move) {
		return true;
	}

	public int[] getRotationDependencies() {
		int[] rdep = new int[myHasher.numElements + 1];
		for (int i = 0; i <= myHasher.numElements; i++)
			rdep[i] = i;
		return rdep;
	}

	public void rotateToBaseState(GenHasher<S> hasher, S state, S rotateTo,
			int numChanged) {
		hasher.set(state, rotateTo, numChanged);
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
	public Collection<Range<S>> getRoots() {
		return getRoots(false);
	}

	public Range<S> makeContainingRange(S t) {
		return makeContainingRange(t, false);
	}

	public Range<S> makeContainingRange(S t, boolean output) {
		Range<S> range = newRange();
		if (output)
			makeOutputContainingRange(t, range);
		else
			makeContainingRange(t, range);
		return range;
	}

	public void makeContainingRange(S t, Range<S> range) {
		range.set(t, suffLen);
	}

	public void makeOutputContainingRange(S t, Range<S> range) {
		range.set(t, innerSuffixLength());
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
		QLLFactory<SaveMove> fact = new QLLFactory<SaveMove>();
		currentMoves = fact.getList();
		movePlaces = new QuickLinkedList[varLen];
		for (int i = 0; i < varLen; i++) {
			movePlaces[i] = fact.getList();
		}
		rotDep = getRotationDependencies();
		assert rotDep.length == myHasher.numElements + 1;
		assert rotDep[varLen] == varLen;
		lastChanged = new int[varLen];
		state = myHasher.newState();
	}

	protected void innerConfigure(Configuration conf) {
	}

	public abstract GenHasher<S> getHasher();

	protected abstract Move[] getMoves();

	protected abstract int suffixLength();

	public GameRecord getRecord(Range<S> range, S state, MainRecords records) {
		long iVal = range.subHash(getHasher(), state);
		assert iVal <= Integer.MAX_VALUE;
		return records.get((int) iVal);
	}

	public int innerSuffixLength() {
		return suffixLength();
	}

	public Collection<Range<S>> getRoots(boolean output) {
		Collection<S> startingPositions = getStartingPositions();
		HashSet<Range<S>> containingRanges = new HashSet<Range<S>>();
		for (S t : startingPositions) {
			Range<S> containingRange;
			containingRange = makeContainingRange(t, output);
			containingRanges.add(containingRange);
		}
		return containingRanges;
	}

	@Override
	public Class<? extends RangeTreeNode<S>> getTreeNodeClass() {
		return (Class<? extends RangeTreeNode<S>>) RangeTreeNode.class
				.<RangeTreeNode> asSubclass(RangeTreeNode.class);
	}
}
