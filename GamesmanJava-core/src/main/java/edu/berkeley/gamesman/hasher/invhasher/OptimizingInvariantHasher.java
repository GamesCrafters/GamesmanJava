package edu.berkeley.gamesman.hasher.invhasher;

import java.util.Arrays;

import edu.berkeley.gamesman.hasher.genhasher.GenState;

/**
 * Whereas the only assumption made by the InvariantHasher is that if two
 * prefixes have the same invariant, they must have the same number of
 * completions, the OptimizingInvariantHasher also assumes that if two prefixes
 * have the same invariant and the next element is the same, then the prefixes
 * plus the element must have the same number of completions. Note that this
 * must include cases where the last element of the prefix is invalid (ie. if
 * two prefixes have the same invariant, then the set of elements which are
 * invalid following the prefix must be the same for both)
 * 
 * @author dnspies
 * 
 * @param <S>
 *            The state type
 */
public abstract class OptimizingInvariantHasher<S extends GenState> extends
		InvariantHasher<S> {
	private long[][][] invariantVals;

	/**
	 * @param numElements
	 * @param digitBase
	 * @param initState
	 */
	public OptimizingInvariantHasher(int numElements, int digitBase) {
		super(numElements, digitBase);
		invariantVals = new long[numElements][][];
	}

	@Override
	protected long sigValue(S state) {
		int place = getStart(state);
		if (invariantVals[place] == null) {
			int numInvs = numInvariants(place);
			invariantVals[place] = new long[numInvs][baseFor(place)];
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
	protected int lastInvariant(S state) {
		int ls = leastSig(state);
		trunc(state);
		int result = getInvariant(state);
		assert result >= 0;
		addLS(state, ls);
		return result;
	}
}
