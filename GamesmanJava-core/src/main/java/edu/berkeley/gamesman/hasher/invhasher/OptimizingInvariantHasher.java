package edu.berkeley.gamesman.hasher.invhasher;

import java.util.HashMap;

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
	private HashMap<Long, Long>[][] invariantVals;

	public OptimizingInvariantHasher(int[] digitBase) {
		this(digitBase, 0);
	}

	/**
	 * @param numElements
	 * @param digitBase
	 * @param countingPlace
	 * @param initState
	 */
	public OptimizingInvariantHasher(int[] digitBase, int countingPlace) {
		super(digitBase, countingPlace);
		invariantVals = new HashMap[numElements][];
		for (int i = 0; i < numElements; i++) {
			invariantVals[i] = new HashMap[digitBase[i]];
			for (int j = 0; j < digitBase[i]; j++)
				invariantVals[i][j] = new HashMap<Long, Long>();
		}
	}

	@Override
	protected long sigValue(S state) {
		int place = getStart(state);
		int ls = leastSig(state);
		long lastInv = lastInvariant(state);
		assert lastInv >= 0;
		Long count = invariantVals[place][ls].get(lastInv);
		if (count == null) {
			count = super.sigValue(state);
			invariantVals[place][ls].put(lastInv, count);
		}
		return count;
	}

	/**
	 * @param state
	 * @return
	 */
	protected long lastInvariant(S state) {
		int ls = leastSig(state);
		trunc(state);
		long result = getInvariant(state);
		assert result >= 0;
		addLS(state, ls);
		return result;
	}
}
