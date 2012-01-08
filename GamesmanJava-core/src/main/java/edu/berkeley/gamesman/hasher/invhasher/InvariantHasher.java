package edu.berkeley.gamesman.hasher.invhasher;

import java.util.Arrays;

import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;

public abstract class InvariantHasher<S extends GenState> extends GenHasher<S> {
	private long[][] invariantCounts;

	/**
	 * @param numElements
	 * @param digitBase
	 * @param initState
	 */
	public InvariantHasher(int[] digitBase) {
		super(digitBase);
		invariantCounts = new long[numElements + 1][];
	}

	@Override
	protected long innerCountCompletions(S state) {
		int start = getStart(state);
		if (invariantCounts[start] == null) {
			invariantCounts[start] = new long[numInvariants(start)];
			Arrays.fill(invariantCounts[start], -1L);
		}
		int inv = getInvariant(state);
		if (inv < 0)
			return 0L;
		if (invariantCounts[start][inv] >= 0) {
			return invariantCounts[start][inv];
		}
		if (isComplete(state)) {
			if (valid(state))
				invariantCounts[start][inv] = 1L;
			else
				invariantCounts[start][inv] = 0L;
		} else {
			S tempState = getPoolPref();
			tempState.set(state);
			assert validLS(tempState);
			addOn(tempState, false);
			long posCount = 0L;
			do {
				posCount += countCompletions(tempState);
			} while (incr(tempState, 1));
			releasePref(tempState);
			invariantCounts[start][inv] = posCount;
		}
		return invariantCounts[start][inv];
	}

	/**
	 * @param state
	 * @return
	 */
	protected abstract int getInvariant(S state);

	@Override
	protected abstract boolean valid(S state);

	/**
	 * @param startPoint
	 * @return
	 */
	protected abstract int numInvariants(int startPoint);
}
