package edu.berkeley.gamesman.parallel.ranges;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.ReflectionUtils;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.hasher.genhasher.Move;
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

/**
 * Corresponds to games of a specific format.<br />
 * Where the state can be hashed using the GenHasher and the moves are a small
 * set of combinations of replacements (ie can be specified using the Move
 * interface)
 * 
 * @author dnspies
 * 
 * @param <S>
 *            The state type
 */
public abstract class RangeTree<S extends GenState> extends
		SimpleTree<Suffix<S>, MainRecords, ChildMap, RecordMap> {

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
	private boolean useRotation;

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
					t.map = null;
				}
			});

	/**
	 * Return the W/L/T value of the game for a given primitive state. If the
	 * state is not primitive, return null.
	 * 
	 * @param state
	 *            The (possibly) primitive position
	 * @return W/L/T or null (if not primitive)
	 */
	public abstract GameValue getValue(S state);

	@Override
	public void firstVisit(Suffix<S> key, MainRecords valueToFill,
			WritList<Entry<Suffix<S>, ChildMap>> parents,
			Adder<Entry3<Suffix<S>, RecordMap, ChildMap>> childrenToFill) {
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
						int matches = sm.move.matches(state);
						if (matches == -1) {
							if (validMove(state, sm.move)) {
								int numChanged;
								S childState;
								if (sm.map == null)
									numChanged = myHasher.numElements;
								else {
									numChanged = 0;
									while (numChanged < varLen
											&& sm.lastParentPosition < lastChanged[numChanged])
										numChanged++;
								}
								myHasher.makeMove(state, sm.move,
										sm.childState, numChanged);
								if (useRotation) {
									rotateToBaseState(myHasher, sm.childState,
											sm.rotatedState, numChanged);
									childState = sm.rotatedState;
								} else
									childState = sm.childState;
								if (sm.map == null) {
									Entry3<Suffix<S>, RecordMap, ChildMap> entry = childrenToFill
											.add();
									entry.getT1().set(childState, suffLen);
									entry.getT2().clear();
									ChildMap childMap = entry.getT3();
									childMap.clear();
									sm.map = childMap;
									numChanged = varLen;
								}
								long childHash = myHasher.hash(childState,
										sm.childSubHashes, rotDep[numChanged]);
								assert childHash == myHasher.hash(childState,
										null, varLen);
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

	protected int[] getRotationDependencies() {
		int[] rdep = new int[myHasher.numElements + 1];
		for (int i = 0; i <= myHasher.numElements; i++)
			rdep[i] = i;
		return rdep;
	}

	public void rotateToBaseState(GenHasher<S> hasher, S state, S rotateTo,
			int numChanged) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void combineDown(Suffix<S> key, MainRecords value,
			WritList<Entry<Suffix<S>, ChildMap>> parents, int firstNewParent,
			WritableList<Entry<Suffix<S>, RecordMap>> children) {
	}

	private final QuickLinkedList<RecordMap> current = new QuickLinkedList<RecordMap>();
	private final QuickLinkedList<RecordMap> waiting = new QuickLinkedList<RecordMap>();

	@Override
	public boolean combineUp(Suffix<S> key, MainRecords value,
			WritList<Entry<Suffix<S>, ChildMap>> parents,
			WritableList<Entry<Suffix<S>, RecordMap>> children) {
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
	public void sendUp(Suffix<S> key, MainRecords value, Suffix<S> parentKey,
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
	public Class<Suffix<S>> getKeyClass() {
		return (Class<Suffix<S>>) Suffix.class
				.<Suffix> asSubclass(Suffix.class);
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
	public Collection<Suffix<S>> getRoots() {
		return getRoots(false);
	}

	public Suffix<S> makeContainingRange(S t) {
		return makeContainingRange(t, false);
	}

	public Suffix<S> makeContainingRange(S t, boolean output) {
		Suffix<S> range = newRange();
		if (output)
			makeOutputContainingRange(t, range);
		else
			makeContainingRange(t, range);
		return range;
	}

	public void makeContainingRange(S t, Suffix<S> range) {
		range.set(t, suffLen);
	}

	public void makeOutputContainingRange(S t, Suffix<S> range) {
		range.set(t, outputSuffixLength());
	}

	private final Suffix<S> newRange() {
		return ReflectionUtils.newInstance(getKeyClass(), getConf());
	}

	/**
	 * @return The possible starting positions for a game. In case the game has
	 *         different variants which may be solved simultaneously, you may
	 *         have more than one.
	 */
	public abstract Collection<S> getStartingPositions();

	@Override
	public final void configure(Configuration conf) {
		rangeTreeConfigure(conf);
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
		useRotation = usesRotation();
		rotDep = getRotationDependencies();
		assert rotDep.length == myHasher.numElements + 1;
		assert rotDep[varLen] == varLen;
		lastChanged = new int[varLen];
		state = myHasher.newState();
	}

	protected boolean usesRotation() {
		return false;
	}

	protected void rangeTreeConfigure(Configuration conf) {
	}

	/**
	 * @return The hasher for this game
	 */
	public abstract GenHasher<S> getHasher();

	/**
	 * @return An array consisting of all possible moves for this game from any
	 *         position. If further discrimination is needed, override
	 *         validMove()
	 */
	protected abstract Move[] getMoves();

	/**
	 * The suffix length is the number of pieces which are fixed in a given
	 * range.<br />
	 * Note that if you're handling symmetries, you should expect only certain
	 * values will work.
	 */
	protected abstract int suffixLength();

	public GameRecord getRecord(Suffix<S> range, S state, MainRecords records) {
		long iVal = range.subHash(getHasher(), state);
		assert iVal <= Integer.MAX_VALUE;
		return records.get((int) iVal);
	}

	/**
	 * This is the length of the suffix in the final output database. In general
	 * it should be the same or larger than the suffix length when solving. This
	 * makes it easier to read positions from the database, since only smaller
	 * ranges need to be read.
	 * 
	 * @return
	 */
	public int outputSuffixLength() {
		return suffixLength();
	}

	public Collection<Suffix<S>> getRoots(boolean output) {
		Collection<S> startingPositions = getStartingPositions();
		HashSet<Suffix<S>> containingRanges = new HashSet<Suffix<S>>();
		for (S t : startingPositions) {
			Suffix<S> containingRange;
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

	@Override
	public Class<? extends Reducer> getCleanupReducerClass() {
		return RangeReducer.class;
	}
}
