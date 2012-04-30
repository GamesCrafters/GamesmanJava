package edu.berkeley.gamesman.hasher.genhasher;

import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;

/**
 * A very generalized hasher for sequences where it's possible to count the
 * number of ways of completing a given sequence subject to constraints
 * 
 * @author dnspies
 * @param <S>
 *            The state type to be hashed
 */
public abstract class GenHasher<S extends GenState> {

	private static boolean superAsserts = false;
	/**
	 * The length of the sequences
	 */
	public final int numElements;
	/**
	 * The number of possible digits for each element of the sequence
	 */
	private final int[] digitBase;

	private long allPositions = -1L;

	// private final Pool<S> statePool = new Pool<S>(new Factory<S>() {
	// @Override
	// public S newObject() {
	// return newState();
	// }
	//
	// @Override
	// public void reset(S t) {
	// assert validTest(t);
	// }
	// });
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
	 *            The length of the sequences
	 * @param digitBase
	 *            The number of possible digits for each element of the sequence
	 */
	public GenHasher(int[] digitBase) {
		this.numElements = digitBase.length;
		this.digitBase = new int[numElements];
		System.arraycopy(digitBase, 0, this.digitBase, 0, numElements);
	}

	/**
	 * Hashes a given state
	 * 
	 * @param state
	 *            The state to be hashed
	 * @return The hash of the state
	 */
	public final long hash(S state) {
		return hash(state, null, numElements);
	}

	/**
	 * Returns the hash of the pieces up to suffixStart
	 * 
	 * @param state
	 *            The state to hash
	 * @param suffixStart
	 *            The place to stop hashing at (exclusive)
	 * @return The hash of the pieces up to the start of the suffix
	 */
	public final long hash(S state, long[] valAt, int upTo) {
		assert validTest(state);
		S state2 = null;
		long hash;
		if (superAsserts) {
			state2 = getPoolPref();
			state2.set(state);
		}
		try {
			if (valAt == null || upTo == valAt.length)
				hash = 0L;
			else
				hash = valAt[upTo];
			state.trunc(upTo);
			while (!state.isComplete()) {
				state.unsafeAdd();
				hash += sigValue(state);
				if (valAt != null)
					valAt[state.getStart()] = hash;
			}
			assert !superAsserts || state.equals(state2);
		} finally {
			if (superAsserts) {
				releasePref(state2);
			}
		}
		return hash;
	}

	/**
	 * @param state
	 *            If debugging is turned on, tests that the state is valid
	 * @return Whether the state is valid
	 */
	protected final boolean validTest(S state) {
		return !superAsserts || totalValid(state);
	}

	/**
	 * Tests validity of the entire state (without making any assumtions).
	 * Override this if valid() doesn't sufficiently check any state. It will
	 * greatly simplify debugging
	 * 
	 * @param state
	 *            The state to test
	 * @return Whether the state is valid
	 */
	protected boolean totalValid(S state) {
		return valid(state);
	}

	private final boolean validPrefTest(S state) {
		return !superAsserts || totalValidPref(state);
	}

	/**
	 * Tests validity of state prefix without making assumptions. Override this
	 * if validPref() doesn't sufficiently check any prefix. It will greatly
	 * simplify debugging.
	 * 
	 * @param state
	 *            The prefix to test
	 * @return Whether the prefix is valid
	 */
	protected boolean totalValidPref(S state) {
		return validPref(state);
	}

	/**
	 * @param state
	 *            Computes the hash contribution for the lowest digit in this
	 *            prefix
	 * @return The amount which this digit contributes to the hash
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

	/**
	 * Unhashes a state and stores it in fillState
	 * 
	 * @param hash
	 *            The hash of the state
	 * @param fillState
	 *            A state object to be filled
	 */
	public final void unhash(long hash, S fillState) {
		fillState.clear();
		while (!fillState.isComplete()) {
			fillState.addOn(false);
			hash -= raiseLS(fillState, hash);
			assert validPrefTest(fillState);
		}
		assert hash == 0;
	}

	/**
	 * Determines the next digit for a given prefix and remaining hash to be
	 * used
	 * 
	 * @param state
	 *            The state to add the next digit to
	 * @param hash
	 *            The remaining amount of hash to be used
	 * @return The amount of hash which has been used
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
	 * Takes a state with hash h and modifies it so it hashes to h+1
	 * 
	 * @param state
	 *            The state to modify
	 * @return The index n of the smallest-index piece such that for all m>=n
	 *         piece m was not changed.
	 */
	public final int step(S state) {
		return step(state, 1);
	}

	public final long step(S state, int dir, int minChange) {
		long initialHash = 0L;
		assert (initialHash = hash(state)) != -1;
		long diff = 0;
		while (state.getStart() < minChange) {
			diff -= sigValue(state);
			state.trunc();
		}
		if (dir == 1)
			diff += countCompletions(state);
		else if (dir == -1)
			diff--;
		else
			throw new RuntimeException("dir can only be 1 or -1");
		int place = step(state, dir);
		validComplete(state, dir == -1);
		assert place == -1 || hash(state) - initialHash == diff;
		return diff;
	}

	public final int step(S state, int dir) {
		assert dir == 1 || dir == -1;
		int truncTimes = 0;
		try {
			while (true) {
				if (state.isEmpty())
					return -1;
				boolean incred = state.incr(dir);
				if (incred) {
					incred = incToValid(state, dir);
				}
				if (incred)
					break;
				state.trunc();
				truncTimes++;
			}
		} finally {
			for (int i = 0; i < truncTimes; i++)
				addValid(state, dir == -1);
			assert validPrefTest(state);
		}
		return state.getStart() + truncTimes;
	}

	/**
	 * Returns whether the given state prefix is a prefix for any valid state
	 * 
	 * @param state
	 *            The prefix
	 * @return Whether there exists a state for which this prefix is valid
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

	/**
	 * Resets the first element to the first valid digit. This should only be
	 * called if trunc(state) followed by validPref(state) would return true
	 * 
	 * @param state
	 *            The state to reset
	 * @param startHigh
	 *            Whether to start counting down from the top or (if false)
	 *            counting up from the bottom
	 */
	protected final void resetValid(S state, boolean startHigh) {
		state.resetLS(startHigh);
		boolean inced = incToValid(state, startHigh ? -1 : 1);
		assert inced;
		assert validPrefTest(state);
	}

	/**
	 * Increments the current digit until it reaches a position for which this
	 * prefix is valid.
	 * 
	 * @param state
	 *            The state to increment
	 * @param dir
	 *            The direction (-1 or 1)
	 * @return true if it reaches a valid prefix. false if it exhausts all the
	 *         remaining digits
	 */
	protected boolean incToValid(S state, int dir) {
		while (!validPref(state)) {
			boolean incred = state.incr(dir);
			if (!incred)
				return false;
		}
		return true;
	}

	/**
	 * Adds the lowest valid remaining digits on to make a valid complete
	 * sequence
	 * 
	 * @param state
	 *            The state to modify
	 * @param startHigh
	 *            Whether to add the highest possible digits or the lowest
	 *            possible digits
	 */
	protected void validComplete(S state, boolean startHigh) {
		while (!state.isComplete()) {
			addValid(state, startHigh);
		}
		assert validTest(state);
	}

	/**
	 * Counts the number of possible positions which have the given state prefix
	 * (state.startAt indicates where the prefix starts).
	 * 
	 * @param state
	 *            The prefix to count.
	 * @return The number of ways of completing this prefix
	 */
	protected final long countCompletions(S state) {
		if (state.validLS())
			return innerCountCompletions(state);
		else
			return 0;
	}

	/**
	 * Counts the number of ways of completing a given state-prefix. This method
	 * is crucial to being able to hash. Note that if state is complete, then
	 * this method should return 1 (for valid) or 0 (for invalid).
	 * 
	 * @param state
	 *            A (possibly incomplete) state
	 * @return The number of positions possible with the given state prefix
	 */
	protected abstract long innerCountCompletions(S state);

	/**
	 * @return The total number of positions hashed by this hasher
	 */
	public final long totalPositions() {
		if (allPositions == -1) {
			allPositions = numPositions(null, 0);
		}
		return allPositions;
	}

	/**
	 * @return A new complete valid instance of type S
	 */
	public final S newState() {
		S res = genHasherNewState();
		assert validTest(res);
		return res;
	}

	/**
	 * Must return a complete state
	 * 
	 * @return Returns a new state
	 */
	protected abstract S genHasherNewState();

	public static void enableToughAsserts() {
		superAsserts = true;
	}

	// /**
	// * @return A state from the inner state pool. Use release to return it to
	// * the pool.
	// */
	// public final S getPoolState() {
	// S state = statePool.get();
	// assert validTest(state);
	// return state;
	// }

	/**
	 * @return A state from the prefix pool. This one is not validated so it may
	 *         be bad and doesn't need to be reset before being returned.
	 */
	protected final S getPoolPref() {
		return interStatePool.get();
	}

	// /**
	// * Releases a state back to the pool. It should be valid and complete
	// *
	// * @param poolState
	// * The state to release
	// */
	// public final void release(S poolState) {
	// assert validTest(poolState);
	// assert poolState.hasHasher(this);
	// statePool.release(poolState);
	// }

	/**
	 * Releases a state back to the prefix pool. It does not need to be valid or
	 * complete.
	 * 
	 * @param poolPref
	 *            The state to release.
	 */
	protected final void releasePref(S poolPref) {
		interStatePool.release(poolPref);
	}

	/**
	 * @param state
	 *            The state
	 * @return The place where the prefix starts for this state
	 */
	protected final int getStart(S state) {
		return state.getStart();
	}

	/**
	 * This is equivalent to getStart(state)==0
	 * 
	 * @param state
	 *            The state
	 * @return Whether the state is complete.
	 */
	protected final boolean isComplete(S state) {
		return state.isComplete();
	}

	/**
	 * This is equivalent to get(getStart(state))
	 * 
	 * @param state
	 *            The state
	 * @return The value of the element at the start of this prefix
	 */
	protected final int leastSig(S state) {
		return state.leastSig();
	}

	/**
	 * Adds another element onto the prefix. (ie state.start--)
	 * 
	 * @param state
	 *            The state to add to
	 * @param startHigh
	 *            Whether to make that element as high as possible (true) or as
	 *            low as possible (false)
	 */
	protected final void addOn(S state, boolean startHigh) {
		state.addOn(startHigh);
	}

	/**
	 * Increments leastSig(state) by dir (dir==1 or -1) (This may be overridden
	 * to increment to the next valid position)
	 * 
	 * @param state
	 *            The state to increment
	 * @param dir
	 *            The direction to go (up or down)
	 * @return Whether this digit is still in [0,digBase).
	 */
	protected final boolean incr(S state, int dir) {
		return state.incr(dir);
	}

	/**
	 * Removes an element from the prefix. (ie state.start++)
	 * 
	 * @param state
	 *            The state
	 */
	protected final void trunc(S state) {
		state.trunc();
	}

	/**
	 * Removes all elements up to place from the prefix. <br />
	 * <code>
	 * while (getStart(state) &lt place) {<br />
	 * &nbsp trunc(state);<br />
	 * }<br />
	 * </code>
	 * 
	 * @param state
	 *            The state
	 * @param place
	 *            The place
	 */
	protected final void trunc(S state, int place) {
		state.trunc(place);
	}

	/**
	 * Adds ls onto the prefix (ie state[state.start--]=ls)
	 * 
	 * @param state
	 *            The prefix to add to
	 * @param ls
	 *            The element to add
	 */
	protected final void addLS(S state, int ls) {
		state.addLS(ls);
	}

	/**
	 * Resets the prefix element at start to 0 or digBase-1 (may be overridden
	 * to find first valid position)
	 * 
	 * @param state
	 *            The state
	 * @param startHigh
	 *            Whether to go to digBase-1 (high=true) or 0 (low=false);
	 */
	protected final void resetLS(S state, boolean startHigh) {
		state.resetLS(startHigh);
	}

	/**
	 * Determines if this prefix is empty (ie getStart(state)==numElements)
	 * 
	 * @param state
	 *            The state
	 * @return If it's empty
	 */
	protected final boolean isEmpty(S state) {
		return state.isEmpty();
	}

	public static boolean useToughAsserts() {
		return superAsserts;
	}

	/**
	 * Takes a separate state (possibly from another hasher) and makes a move on
	 * that state which it stores in childState
	 * 
	 * @param firstState
	 *            The parent
	 * @param move
	 *            The move
	 * @param childState
	 *            The state in which to store the result
	 * @param numChanged
	 */
	public final void makeMove(S firstState, Move move, S childState,
			int numChanged) {
		set(firstState, childState, numChanged, false);
		makeMove(childState, move, numChanged);
		assert !superAsserts || sameAsMakeMove(firstState, move, childState);
	}

	private boolean sameAsMakeMove(S firstState, Move move, S childState) {
		S state2 = getPoolPref();
		state2.set(firstState);
		makeMove(state2, move);
		releasePref(state2);
		return childState.equals(state2);
	}

	public final void makeMove(S state, Move move) {
		makeMove(state, move, numElements);
	}

	private final void makeMove(S state, Move move, int numChanged) {
		for (int i = 0; i < move.numChanges(); i++) {
			int place = move.getChangePlace(i);
			if (place >= numChanged)
				break;
			if (state.get(place) == move.getChangeFrom(i))
				state.set(place, move.getChangeTo(i));
			else
				throw new Error("Cannot make this move");
		}
		assert validTest(state);
	}

	/**
	 * Sets the state's internal ints to match seq
	 * 
	 * @param state
	 *            The state to set
	 * @param seq
	 *            The sequence to match
	 */
	public final void set(S state, int[] seq) {
		state.set(seq, 0);
		assert validTest(state);
	}

	public final void set(S state, S seq) {
		state.setOther(seq);
		assert validTest(state);
	}

	/**
	 * Returns whether get(state,getStart(state)) is in [0,digBase)
	 * 
	 * @param state
	 *            The state
	 * @return Whether the LS is a valid digit
	 */
	protected final boolean validLS(S state) {
		return state.validLS();
	}

	public int baseFor(int n) {
		return digitBase[n];
	}

	/**
	 * This will return the number of positions which match the passed suffix
	 * 
	 * @param suffix
	 *            The suffix to count
	 * @param len
	 * @return The number of positions for this suffix
	 */
	public long numPositions(int[] suffix, int len) {
		S pref = getPoolPref();
		pref.clear();
		for (int i = len - 1; i >= 0; i--) {
			if (!validPref(pref))
				break;
			addLS(pref, suffix[i]);
		}
		long numCompletions = countCompletions(pref);
		releasePref(pref);
		return numCompletions;
	}

	public final boolean firstPosition(int[] suffix, int suffLen, S toFill) {
		toFill.clear();
		for (int i = suffLen - 1; i >= 0; i--) {
			toFill.addLS(suffix[i]);
			if (!validPref(toFill))
				return false;
		}
		validComplete(toFill, false);
		return true;
	}

	public final long stepTo(S state, Move move, int cutoff) {
		long startHash = 0;
		assert (startHash = hash(state)) != -1;
		// How do you perform a computation and store it only when asserts are
		// enabled? See here

		long diff = 0;
		int place = move.matches(state);
		while (state.getStart() < place) {
			diff -= sigValue(state);
			state.trunc();
		}
		int changedPlace = place;
		while (changedPlace < cutoff && place != -1) {
			if (state.getStart() < place) {
				state.trunc(place);
			}
			diff += countCompletions(state);
			changedPlace = step(state, 1);
			place = move.matches(state);
			if (place == -1) {
				validComplete(state, false);
				place = move.matches(state);
			}
		}
		if (changedPlace >= cutoff)
			return -1;
		else {
			assert diff == hash(state) - startHash;
			return diff;
		}
	}

	public long numPositions(int[] pieces) {
		return numPositions(pieces, pieces.length);
	}

	public void set(S from, S to, int numChanged) {
		set(from, to, numChanged, true);
	}

	private void set(S from, S to, int numChanged, boolean assertEquals) {
		for (int i = numChanged - 1; i >= 0; i--) {
			to.set(i, from.get(i));
		}
		assert !assertEquals || from.equals(to);
	}

}
