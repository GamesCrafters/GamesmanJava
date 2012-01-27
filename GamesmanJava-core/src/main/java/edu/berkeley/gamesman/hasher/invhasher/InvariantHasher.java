package edu.berkeley.gamesman.hasher.invhasher;

import java.util.HashMap;

import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;

public abstract class InvariantHasher<S extends GenState> extends GenHasher<S> {
	private HashMap<Long, Long>[] invariantCounts;
	private final int countingPlace;

	/**
	 * @param numElements
	 * @param digitBase
	 * @param initState
	 */
	public InvariantHasher(int[] digitBase) {
		this(digitBase, 0);
	}

	public InvariantHasher(int[] digitBase, int countingPlace) {
		super(digitBase);
		invariantCounts = new HashMap[numElements + 1];
		for (int i = 0; i <= numElements; i++)
			invariantCounts[i] = new HashMap<Long, Long>();
		this.countingPlace = countingPlace;
	}

	@Override
	protected long innerCountCompletions(S state) {
		int start = getStart(state);
		long inv = getInvariant(state);
		if (inv < 0)
			return 0L;
		Long count = invariantCounts[start].get(inv);
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
				posCount += countCompletions(tempState);
			} while (incr(tempState, 1));
			releasePref(tempState);
		}
		if (countingPlace == start && posCount > 0)
			posCount = 1L;
		invariantCounts[start].put(inv, posCount);
		return posCount;
	}

	/**
	 * @param state
	 * @return
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

	private long maximum(int i) {
		long max = Long.MIN_VALUE;
		for (Long l : invariantCounts[i].values()) {
			if (l > max)
				max = l;
		}
		return max;
	}
}
