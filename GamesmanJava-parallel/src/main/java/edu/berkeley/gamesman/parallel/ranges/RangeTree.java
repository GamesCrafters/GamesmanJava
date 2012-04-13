package edu.berkeley.gamesman.parallel.ranges;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.util.ReflectionUtils;

import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.hasher.genhasher.Move;
import edu.berkeley.gamesman.parallel.writable.WritableTreeMap;
import edu.berkeley.gamesman.propogater.common.Adder;
import edu.berkeley.gamesman.propogater.common.Entry3;
import edu.berkeley.gamesman.propogater.tree.SimpleTree;
import edu.berkeley.gamesman.propogater.writable.Entry;
import edu.berkeley.gamesman.propogater.writable.FixedLengthWritable;
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
public abstract class RangeTree<S extends GenState, GR extends FixedLengthWritable>
		extends
		SimpleTree<Suffix<S>, MainRecords<GR>, ChildMap, WritableTreeMap<GR>> {

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

	private GR tempRec;

	@Override
	public void firstVisit(
			Suffix<S> key,
			MainRecords<GR> valueToFill,
			WritList<Entry<Suffix<S>, ChildMap>> parents,
			Adder<Entry3<Suffix<S>, WritableTreeMap<GR>, ChildMap>> childrenToFill) {
		boolean hasFirst = key.firstPosition(myHasher, state);
		assert hasFirst;
		valueToFill.reset(true);
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
			if (setNewRecordAndHasChildren(state, tempRec)) {
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
									Entry3<Suffix<S>, WritableTreeMap<GR>, ChildMap> entry = childrenToFill
											.add();
									entry.getT1().set(childState, suffLen);
									entry.getT2().clear(false);
									ChildMap childMap = entry.getT3();
									childMap.clear(true);
									sm.map = childMap;
									numChanged = varLen;
								}
								long childHash = myHasher.hash(childState,
										sm.childSubHashes, rotDep[numChanged]);
								assert childHash == myHasher.hash(childState,
										null, varLen);
								assert childHash <= Integer.MAX_VALUE;
								sm.map.add(pos, (int) childHash);
								sm.lastParentPosition = pos;
							}
						} else {
							sIter.remove();
							if (matches < varLen) {
								movePlaces[matches].add(sm);
							} else {
								if (sm.map != null)
									sm.map.finish();
								movePool.release(sm);
							}
						}
					}
				} finally {
					currentMoves.release(sIter);
				}
			}
			valueToFill.add(tempRec);
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
		try {
			while (iter.hasNext()) {
				SaveMove sm = iter.next();
				if (sm.map != null)
					sm.map.finish();
				movePool.release(sm);
			}
		} finally {
			currentMoves.release(iter);
		}
		currentMoves.clear();
	}

	protected abstract boolean setNewRecordAndHasChildren(S state, GR rec);

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
	public void combineDown(Suffix<S> key, MainRecords<GR> value,
			WritList<Entry<Suffix<S>, ChildMap>> parents, int firstNewParent,
			WritableList<Entry<Suffix<S>, WritableTreeMap<GR>>> children) {
	}

	private final QuickLinkedList<WritableTreeMap<GR>> current = new QuickLinkedList<WritableTreeMap<GR>>();
	private final QuickLinkedList<WritableTreeMap<GR>> waiting = new QuickLinkedList<WritableTreeMap<GR>>();
	private final QuickLinkedList<GR> grList = new QuickLinkedList<GR>();

	@Override
	public boolean combineUp(Suffix<S> key, MainRecords<GR> value,
			WritList<Entry<Suffix<S>, ChildMap>> parents,
			WritableList<Entry<Suffix<S>, WritableTreeMap<GR>>> children) {
		int numPositions = value.length();
		boolean changed = false;
		current.clear();
		waiting.clear();
		for (int i = 0; i < children.length(); i++) {
			WritableTreeMap<GR> child = children.get(i).getValue();
			if (child != null) {
				child.restart();
				current.push(child);
			}
		}
		for (int pos = 0; pos < numPositions; pos++) {
			grList.clear();
			while (!waiting.isEmpty() && waiting.peek().peekNext() <= pos) {
				current.push(waiting.pop());
			}
			QuickLinkedList<WritableTreeMap<GR>>.QLLIterator iter = current
					.iterator();
			try {
				while (iter.hasNext()) {
					WritableTreeMap<GR> child = iter.next();
					GR gvalue = child.getNext(pos);
					if (gvalue == null) {
						iter.remove();
						int childNext = child.peekNext();
						if (childNext >= 0) {
							assert childNext > pos;
							QuickLinkedList<WritableTreeMap<GR>>.QLLIterator waitingIter = waiting
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
						grList.add(gvalue);
					}
				}
			} finally {
				current.release(iter);
			}
			GR grValue = value.get(pos);
			if (combineValues(grList, grValue)) {
				value.writeBack(pos, grValue);
				changed = true;
			}
		}
		return changed;
	}

	protected abstract boolean combineValues(QuickLinkedList<GR> grList, GR gr);

	@Override
	public void sendUp(Suffix<S> key, MainRecords<GR> value,
			Suffix<S> parentKey, ChildMap parentInfo, WritableTreeMap<GR> toFill) {
		toFill.clear(true);
		parentInfo.restart();
		int nextParentNum = parentInfo.parent();
		int lastParentNum = -1;
		while (nextParentNum >= 0) {
			assert nextParentNum > lastParentNum;
			int nextChildNum = parentInfo.child();
			previousPosition(value.get(nextChildNum), tempRec);
			toFill.add(nextParentNum, tempRec);
			parentInfo.next();
			lastParentNum = nextParentNum;
			nextParentNum = parentInfo.parent();
		}
		toFill.finish();
	}

	protected abstract void previousPosition(GR gr, GR toFill);

	@Override
	public Class<Suffix<S>> getKeyClass() {
		return (Class<Suffix<S>>) Suffix.class
				.<Suffix> asSubclass(Suffix.class);
	}

	@Override
	public Class<MainRecords<GR>> getValClass() {
		return (Class<MainRecords<GR>>) MainRecords.class
				.<MainRecords> asSubclass(MainRecords.class);
	}

	@Override
	public Class<ChildMap> getPiClass() {
		return ChildMap.class;
	}

	@Override
	public Class<WritableTreeMap<GR>> getCiClass() {
		return (Class<WritableTreeMap<GR>>) WritableTreeMap.class
				.<WritableTreeMap> asSubclass(WritableTreeMap.class);
	}

	@Override
	protected void treePrepareRun(Configuration conf) {
		conf.setClass("propogater.run.record.class", getGameRecordClass(),
				Writable.class);
	}

	protected abstract Class<GR> getGameRecordClass();

	public Suffix<S> makeContainingRange(S t) {
		Suffix<S> range = newRange();
		makeContainingRange(t, range);
		return range;
	}

	public void makeContainingRange(S t, Suffix<S> range) {
		range.set(t, suffLen);
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
		suffLen = suffixLength(conf);
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
		tempRec = newRecord();
	}

	protected GR newRecord() {
		return ReflectionUtils.newInstance(getGameRecordClass(), getConf());
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
	protected int suffixLength(Configuration conf) {
		int varianceLength = Math.min(conf.getInt(
				"gamesman.game.variance.length", defaultVarianceLength()),
				maxVarianceLength());
		return getHasher().numElements - varianceLength;
	}

	protected int maxVarianceLength() {
		return getHasher().numElements;
	}

	protected int defaultVarianceLength() {
		return 10;
	}

	protected final int suffLen() {
		return suffLen;
	}

	public GR getRecord(Suffix<S> range, S state, MainRecords<GR> records) {
		GenHasher<S> hasher = getHasher();
		S myState = hasher.newState();
		hasher.set(myState, state);
		long iVal = range.subHash(getHasher(), myState);
		assert iVal <= Integer.MAX_VALUE;
		return records.get((int) iVal);
	}

	@Override
	public Collection<Suffix<S>> getRoots() {
		Collection<S> startingPositions = getStartingPositions();
		HashSet<Suffix<S>> containingRanges = new HashSet<Suffix<S>>();
		for (S t : startingPositions) {
			Suffix<S> containingRange;
			containingRange = makeContainingRange(t);
			containingRanges.add(containingRange);
		}
		return containingRanges;
	}

	@Override
	public Class<? extends RangeTreeNode<S, GR>> getTreeNodeClass() {
		return (Class<? extends RangeTreeNode<S, GR>>) RangeTreeNode.class
				.<RangeTreeNode> asSubclass(RangeTreeNode.class);
	}

	public static <GR extends Writable> Class<GR> getRunGRClass(
			Configuration conf) {
		Class<GR> result = (Class<GR>) conf.getClass(
				"propogater.run.record.class", null, Writable.class);
		if (result == null)
			throw new NullPointerException();
		else
			return result;
	}

	@Override
	public SequenceFile.CompressionType getCleanupCompressionType() {
		return SequenceFile.CompressionType.RECORD;
	}
}
