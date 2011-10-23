package edu.berkeley.gamesman.hasher.invhasher;

import java.util.Arrays;

import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;

/**
 * @author dnspies
 * 
 * @param <S>
 */
public abstract class InvariantHasher<S extends GenState> extends GenHasher<S> {
	private long[][] invariantCounts;
	private long[][][] invariantVals;

	/**
	 * @param numElements
	 * @param digitBase
	 * @param initState
	 */
	public InvariantHasher(int numElements, int digitBase) {
		super(numElements, digitBase);
		invariantCounts = new long[numElements + 1][];
		invariantVals = new long[numElements][][];
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

	@Override
	protected long sigValue(S state) {
		int place = getStart(state);
		if (invariantVals[place] == null) {
			int numInvs = numInvariants(place);
			invariantVals[place] = new long[numInvs][digitBase];
			for (long[] arr : invariantVals[place]) {
				Arrays.fill(arr, -1);
			}
		}
		int lastInv = lastInvariant(state);
		assert lastInv >= 0;
		int ls = leastSig(state);
		if (invariantVals[place][lastInv][ls] == -1)
			invariantVals[place][lastInv][ls] = super.sigValue(state);
		return invariantVals[place][lastInv][ls];
	}

	/**
	 * @param state
	 * @return
	 */
	protected abstract int getInvariant(S state);

	/**
	 * @param state
	 * @return
	 */
	protected int lastInvariant(S state) {
		int ls = leastSig(state);
		trunc(state);
		int result = getInvariant(state);
		assert result >= 0;
		addLS(state, ls);
		return result;
	}

	@Override
	protected abstract boolean valid(S state);

	/**
	 * @param startPoint
	 * @return
	 */
	protected abstract int numInvariants(int startPoint);
}
