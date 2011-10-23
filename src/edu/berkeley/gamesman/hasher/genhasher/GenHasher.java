package edu.berkeley.gamesman.hasher.genhasher;

import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.hasher.cachehasher.CacheMove;

/**
 * A very generalized hasher for sequences where it's possible to count the
 * remaining given a prefix
 * 
 * @author dnspies
 * @param <S>
 *            The state type to be hashed
 */
public abstract class GenHasher<S extends GenState> {

	private static boolean superAsserts = false;
	/**
	 * 
	 */
	public final int numElements;
	/**
	 * 
	 */
	public final int digitBase;

	private long allPositions = -1L;

	private final Pool<S> statePool = new Pool<S>(new Factory<S>() {
		@Override
		public S newObject() {
			return newState();
		}

		@Override
		public void reset(S t) {
			assert validTest(t);
		}
	});
	private final Pool<S> interStatePool = new Pool<S>(new Factory<S>() {
		@Override
		public S newObject() {
			return newState();
		}

		@Override
		public void reset(S t) {
		}
	});

	/**
	 * @param numElements
	 * @param digitBase
	 * @param initState
	 */
	public GenHasher(int numElements, int digitBase) {
		this.numElements = numElements;
		this.digitBase = digitBase;
	}

	public final long hash(S state) {
		assert validTest(state);
		long hash = innerHash(state);
		assert validTest(state);
		return hash;
	}

	protected final boolean validTest(S state) {
		return !superAsserts || totalValid(state);
	}

	protected boolean totalValid(S state) {
		return valid(state);
	}

	private final boolean validPrefTest(S state) {
		return !superAsserts || totalValidPref(state);
	}

	protected boolean totalValidPref(S state) {
		return validPref(state);
	}

	/**
	 * @param state
	 * @return
	 */
	protected long innerHash(S state) {
		S tempState = getPoolPref();
		tempState.set(state);
		long total = 0L;
		while (!tempState.isEmpty()) {
			total += sigValue(tempState);
			tempState.trunc();
		}
		releasePref(tempState);
		return total;
	}

	/**
	 * @param state
	 * @return
	 */
	protected long sigValue(S state) {
		int val = state.leastSig();
		long result = 0L;
		state.resetLS(false);
		while (state.leastSig() < val) {
			result += countCompletions(state);
			boolean incred = state.incr(1);
			assert incred;
		}
		assert state.leastSig() == val;
		return result;
	}

	public final void unhash(long hash, S fillState) {
		innerUnhash(hash, fillState);
		assert validTest(fillState);
	}

	/**
	 * @param hash
	 * @param fillState
	 */
	protected void innerUnhash(long hash, S fillState) {
		fillState.clear();
		while (!fillState.isComplete()) {
			fillState.addOn(false);
			hash -= raiseLS(fillState, hash);
			assert validPrefTest(fillState);
		}
		assert hash == 0;
	}

	/**
	 * @param state
	 * @param hash
	 * @return
	 */
	protected long raiseLS(S state, long hash) {
		long result = 0L;
		long countPositions = countCompletions(state);
		while (hash >= result + countPositions) {
			result += countPositions;
			boolean incred = state.incr(1);
			assert incred;
			countPositions = countCompletions(state);
		}
		return result;
	}

	/**
	 * @param state
	 * @return
	 */
	public final int step(S state) {
		return step(state, 1);
	}

	/**
	 * @param state
	 * @param dir
	 * @param changed
	 * @return
	 */
	public final int step(S state, int dir) {
		assert validTest(state);
		int result = innerStep(state, dir);
		assert validTest(state);
		return result;
	}

	/**
	 * @param state
	 * @param dir
	 * @return
	 */
	protected int innerStep(S state, int dir) {
		return basicStep(state, dir);
	}

	// Handles the case where state is invalid
	private int basicStep(S state, int dir) {
		assert dir == 1 || dir == -1;
		if (state.isEmpty())
			return -1;
		boolean incred = state.incr(dir);
		int result;
		if (incred) {
			while (!validPref(state)) {
				incred = state.incr(dir);
				if (!incred)
					break;
			}
		}
		if (incred)
			result = state.getStart() + 1;
		else {
			state.trunc();
			result = basicStep(state, dir);
			addValid(state, dir == -1);
		}
		assert validPrefTest(state);
		return result;
	}

	/**
	 * Returns whether the given state suffix is a suffix for any valid state
	 * 
	 * @param state
	 *            The suffix
	 * @param lastOnly
	 * @return Whether there exists a state for which this suffix is valid
	 */
	protected boolean validPref(S state) {
		return countCompletions(state) > 0;
	}

	/**
	 * Returns whether the given state is valid. This method should only be
	 * called for complete states
	 * 
	 * @param state
	 *            The state
	 * @return Whether it's valid
	 */
	protected boolean valid(S state) {
		assert state.isComplete();
		return validPref(state);
	}

	/**
	 * Adds the next valid element onto the end of the state. This should only
	 * be called if validPref(state) would return true
	 * 
	 * @param state
	 *            The state to append to
	 * @param startHigh
	 *            whether to start counting down from the top or (if false)
	 *            counting up from the bottom
	 */
	protected final void addValid(S state, boolean startHigh) {
		state.addOn(startHigh);
		boolean inced = incToValid(state, startHigh ? -1 : 1);
		assert inced;
		assert validPrefTest(state);
	}

	protected final void resetValid(S state, boolean startHigh) {
		state.resetLS(startHigh);
		boolean inced = incToValid(state, startHigh ? -1 : 1);
		assert inced;
		assert validPrefTest(state);
	}

	protected boolean incToValid(S state, int dir) {
		while (!validPref(state)) {
			boolean incred = state.incr(dir);
			if (!incred)
				return false;
		}
		return true;
	}

	/**
	 * @param state
	 * @param startHigh
	 */
	protected void validComplete(S state, boolean startHigh) {
		while (!state.isComplete()) {
			addValid(state, startHigh);
		}
		assert validTest(state);
	}

	protected final long countCompletions(S state) {
		if (state.validLS())
			return innerCountCompletions(state);
		else
			return 0;
	}

	/**
	 * Counts the number of possible positions which have the given state suffix
	 * (state.startAt indicates where the suffix starts).
	 * 
	 * @param state
	 *            A (possibly incomplete) state
	 * @return The number of positions possible with the given state suffix
	 */
	protected abstract long innerCountCompletions(S state);

	/**
	 * @return The total number of positions hashed by this hasher
	 */
	public final long totalPositions() {
		if (allPositions == -1) {
			S state = getPoolPref();
			state.clear();
			allPositions = countCompletions(state);
			releasePref(state);
		}
		return allPositions;
	}

	public final S newState() {
		S res = innerNewState();
		assert validTest(res);
		return res;
	}

	/**
	 * Must return a complete state
	 */
	protected abstract S innerNewState();

	public static void enableToughAsserts() {
		superAsserts = true;
	}

	public final S getPoolState() {
		S state = statePool.get();
		assert validTest(state);
		return state;
	}

	protected final S getPoolPref() {
		return interStatePool.get();
	}

	public final void release(S poolState) {
		assert validTest(poolState);
		assert poolState.hasHasher(this);
		statePool.release(poolState);
	}

	protected final void releasePref(S poolPref) {
		interStatePool.release(poolPref);
	}

	protected final int getStart(GenState state) {
		return state.getStart();
	}

	protected final boolean isComplete(S state) {
		return state.isComplete();
	}

	protected final int leastSig(S state) {
		return state.leastSig();
	}

	protected final void addOn(S state, boolean startHigh) {
		state.addOn(startHigh);
	}

	protected final boolean incr(S state, int dir) {
		return state.incr(dir);
	}

	protected final void trunc(S state) {
		state.trunc();
	}

	protected final void trunc(S state, int place) {
		state.trunc(place);
	}

	protected final void addLS(S state, int ls) {
		state.addLS(ls);
	}

	protected final void resetLS(S state, boolean startHigh) {
		state.resetLS(startHigh);
	}

	protected final boolean isEmpty(S state) {
		return state.isEmpty();
	}

	public static boolean useToughAsserts() {
		return superAsserts;
	}

	public final void makeMove(GenState firstState, CacheMove move, S childState) {
		childState.setOther(firstState);
		// This is alright because set does not modify firstState or access any
		// non-final methods which might modify firstState
		makeMove(childState, move);
		assert validTest(childState);
	}

	/**
	 * @param steppingState
	 * @param parentHasher
	 * @param parentState
	 * @param move
	 * @param same
	 * @param dir
	 * @return
	 */
	public final <T extends GenState> long stepTo(S steppingState,
			GenHasher<T> parentHasher, T parentState, CacheMove move, int same,
			int dir) {
		assert !useToughAsserts() || steppingState.matches(parentState, same);
		long diff = 0;
		for (int i = 0; i < same - 1; i++) {
			diff -= sigValue(steppingState);
			steppingState.trunc();
		}
		parentHasher.makeMove(parentState, move);
		while (true) {
			while (steppingState.leastSig() != parentState.get(steppingState
					.getStart())) {
				diff += countCompletions(steppingState);
				boolean incred = steppingState.incr(dir);
				assert incred;
			}
			if (steppingState.isComplete())
				break;
			steppingState.addOn(dir == -1);
		}
		parentHasher.unmakeMove(parentState, move);
		assert parentHasher.validTest(parentState);
		assert validTest(steppingState);
		return diff;
	}

	/**
	 * @param parentHasher
	 * @param parentState
	 * @param move
	 * @param dir
	 * @return
	 */
	public final <T extends GenState> long getChildBound(
			GenHasher<T> parentHasher, T parentState, CacheMove move, int dir) {
		assert dir == 1 || dir == -1;
		assert parentHasher.validTest(parentState);
		int pMove = move.numChanges - 1;
		S childState = getPoolPref();
		childState.clear();
		boolean clobbered = false;
		while (validPref(childState) && !childState.isComplete()) {
			int nextStart = childState.getStart() - 1;
			int nextLS = parentState.get(nextStart);
			if (pMove >= 0 && move.getChangePlace(pMove) == nextStart) {
				int changeFrom = move.getChangeFrom(pMove);
				int changeTo = move.getChangeTo(pMove);
				if (nextLS == changeFrom) {
					childState.addLS(changeTo);
				} else {
					if (dir == -1 && changeFrom > nextLS || dir == 1
							&& changeFrom < nextLS) {
						clobbered = innerStep(childState, dir) == -1;
					}
					if (!clobbered)
						childState.addLS(changeTo);
					break;
				}
				pMove--;
			} else
				childState.addLS(nextLS);
		}
		final long result;
		if (clobbered)
			result = -1;
		else if (isComplete(childState) && valid(childState)) {
			result = hash(childState);
		} else {
			final boolean isValid = validPref(childState)
					|| basicStep(childState, dir) != -1;
			if (isValid) {
				assert validPrefTest(childState);
				validComplete(childState, dir == -1);
				assert validTest(childState);
				result = hash(childState);
			} else
				result = -1;
		}
		releasePref(childState);
		return result;
	}

	final void makeMove(GenState state, CacheMove move) {
		for (int i = 0; i < move.numChanges; i++) {
			if (state.get(move.getChangePlace(i)) == move.getChangeFrom(i))
				state.set(move.getChangePlace(i), move.getChangeTo(i));
			else
				throw new Error("Cannot make this move");
		}
	}

	final void unmakeMove(GenState state, CacheMove move) {
		for (int i = 0; i < move.numChanges; i++) {
			if (state.get(move.getChangePlace(i)) == move.getChangeTo(i))
				state.set(move.getChangePlace(i), move.getChangeFrom(i));
			else
				throw new Error("Cannot unmake this move");
		}
	}

	public final void set(S state, int[] seq) {
		state.set(seq, 0);
		assert validTest(state);
	}

	protected final boolean validLS(S state) {
		return state.validLS();
	}
}
