package edu.berkeley.gamesman.hasher.fixed;

import edu.berkeley.gamesman.hasher.invhasher.OptimizingInvariantHasher;

/**
 * @author dnspies
 * 
 */
public abstract class FixedHasher<S extends FixedState> extends
		OptimizingInvariantHasher<S> implements Restrainer {
	private final int[] numPieces;
	private final int[] multipliers;
	protected final int numInvariants;

	/**
	 * @param numElements
	 * @param digitBase
	 * @param numPieces
	 */
	public FixedHasher(int numElements, int digitBase, int[] numPieces) {
		super(numElements, digitBase);
		this.numPieces = new int[digitBase];
		this.multipliers = new int[digitBase];
		System.arraycopy(numPieces, 0, this.numPieces, 0, digitBase);
		int numInvariants = 1;
		int sumPieces = 0;
		for (int i = 0; i < digitBase; i++) {
			multipliers[i] = numInvariants;
			numInvariants *= numPieces[i] + 1;
			sumPieces += numPieces[i];
		}
		if (sumPieces != numElements)
			throw new Error("Wrong number of total pieces");
		this.numInvariants = numInvariants;
	}

	protected int baseNumInvariants(int startPoint) {
		return numInvariants;
	}

	@Override
	public int numPieces(int digit) {
		return numPieces[digit];
	}

	@Override
	public final int getMult(int dig) {
		return multipliers[dig];
	}

	protected int baseGetInvariant(S state) {
		return state.getInvariant();
	}

	protected int baseLastInvariant(S state) {
		return state.getInvariant() - multipliers[leastSig(state)];
	}

	protected boolean baseValid(S state) {
		assert isComplete(state);
		return validLSAndAllValidCols(state);
	}

	protected boolean baseValidPref(S state) {
		return validLSAndAllValidCols(state);
	}

	/**
	 * @param state
	 * @return
	 */
	protected final boolean validLSAndAllValidCols(S state) {
		return validLS(state) && state.getNumInvalidCols() == 0;
	}
}
