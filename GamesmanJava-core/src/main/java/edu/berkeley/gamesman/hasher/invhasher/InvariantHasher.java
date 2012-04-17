package edu.berkeley.gamesman.hasher.invhasher;

import java.util.HashMap;

import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.util.LongSet;

public abstract class InvariantHasher<S extends GenState> extends GenHasher<S> {
	private HashMap<LongSet, Long>[] invariantCounts;
	private final int countingPlace;
	private final LongSet tempVal = new LongSet();

	/**
	 * @param numElements
	 * @param digitBase
	 * @param initState
	 */
	public InvariantHasher(int[] digitBase) {
		this(digitBase, -1);
	}

	public InvariantHasher(int[] digitBase, int countingPlace) {
		super(digitBase);
		invariantCounts = new HashMap[numElements + 1];
		for (int i = 0; i <= numElements; i++)
			invariantCounts[i] = new HashMap<LongSet, Long>();
		this.countingPlace = countingPlace;
	}

	@Override
	protected long innerCountCompletions(S state) {
		int start = getStart(state);
		long inv = getInvariant(state);
		if (inv < 0)
			return 0L;
		tempVal.value = inv;
		Long count = invariantCounts[start].get(tempVal);
		if (count != null) {
			return count;
		}
		long posCount;
		if (isComplete(state)) {
			if (valid(state)) {
				posCount = 1L;
			} else {
				posCount = 0L;
			}
		} else {
			S tempState = getPoolPref();
			tempState.set(state);
			assert validLS(tempState);
			addOn(tempState, false);
			posCount = 0L;
			do {
				long completions = countCompletions(tempState);
				assert Long.MAX_VALUE - completions > posCount;
				posCount += completions;
			} while (incr(tempState, 1));
			releasePref(tempState);
		}
		if (countingPlace == start) {
			if (posCount > 0)
				posCount = 1;
		}
		Long prevVal = invariantCounts[start].put(new LongSet(inv), posCount);
		assert prevVal == null;
		return posCount;
	}

	/**
	 * This invariant should be associated with the number of ways of completing
	 * this state
	 * 
	 * @param state
	 *            A (possibly incomplete state)
	 * @return A unique number associated with all states that match this in
	 *         some way such that the number of ways of completing it is
	 *         equivalent. (This includes invalid states should not be mixed
	 *         with valid ones)
	 */
	protected abstract long getInvariant(S state);

	@Override
	protected abstract boolean valid(S state);

	public void printStates() {
		long tot = totalPositions();
		System.out.println("Total positions: " + tot);
		for (int i = 0; i <= numElements; i++) {
			System.out.println("Variance Length " + i + ":");
			System.out.println("Maximum size: " + maximum(i));
			System.out.println();
		}
	}

	public long maximum(int i) {
		long max = Long.MIN_VALUE;
		for (Long l : invariantCounts[i].values()) {
			if (l > max)
				max = l;
		}
		return max;
	}
}
