package edu.berkeley.gamesman.hasher.fixed;

import edu.berkeley.gamesman.hasher.counting.CountingState;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;

public class FixedState extends CountingState {
	private final Restrainer mults;
	private int inv;
	private int invalidColCount;

	public FixedState(FixedHasher<?> myHasher, int countTo) {
		super(myHasher, countTo);
		this.mults = myHasher;
		if (countTo > mults.numPieces(0))
			invalidColCount = 1;
		else
			invalidColCount = 0;
		inv = countTo * mults.getMult(0);
		int place = 0;
		for (int i = digitBase() - 1; i >= 0; i--) {
			for (int j = 0; j < mults.numPieces(i); j++) {
				set(place, i);
				place++;
			}
		}
		assert place == countTo;
	}

	/**
	 * @param whichDig
	 */
	protected void incNum(int whichDig) {
		if (numPieces(whichDig) > mults.numPieces(whichDig))
			invalidColCount--;
		super.incNum(whichDig);
		if (numPieces(whichDig) > mults.numPieces(whichDig))
			invalidColCount++;
		inv += mults.getMult(whichDig);
		assert invMatches();
	}

	/**
	 * @param whichDig
	 */
	protected void decNum(int whichDig) {
		assert numPieces(whichDig) > 0;
		if (numPieces(whichDig) > mults.numPieces(whichDig))
			invalidColCount--;
		super.decNum(whichDig);
		if (numPieces(whichDig) > mults.numPieces(whichDig))
			invalidColCount++;
		inv -= mults.getMult(whichDig);
		assert invMatches();
	}

	/**
	 * 
	 */
	protected void clearNums() {
		super.clearNums();
		invalidColCount = 0;
		inv = 0;
		assert invMatches();
	}

	private boolean invMatches() {
		if (!GenHasher.useToughAsserts()) {
			return true;
		}
		int testInv = 0;
		for (int i = 0; i < digitBase(); i++) {
			testInv += numPieces(i) * mults.getMult(i);
		}
		return testInv == inv;
	}

	public int getInvariant() {
		if (invalidColCount == 0 && validLS())
			return inv;
		else
			return -1;
	}

	final int getNumInvalidCols() {
		return invalidColCount;
	}
}
