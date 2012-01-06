package edu.berkeley.gamesman.hasher.counting;

import java.util.Arrays;

import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;

/**
 * @author dnspies
 * 
 */
public class CountingState extends GenState {
	private final int[] numPieces;
	private int[] testing = null;

	/**
	 * @param myHasher
	 */
	public CountingState(GenHasher<? extends CountingState> myHasher) {
		super(myHasher);
		this.numPieces = new int[myHasher.digitBase];
		numPieces[0] = myHasher.numElements;
	}

	/**
	 * @param whichDig
	 */
	protected void incNum(int whichDig) {
		numPieces[whichDig]++;
	}

	/**
	 * @param whichDig
	 */
	protected void decNum(int whichDig) {
		assert numPieces[whichDig] > 0;
		numPieces[whichDig]--;
	}

	/**
	 * 
	 */
	protected void clearNums() {
		Arrays.fill(numPieces, 0);
	}

	@Override
	protected void matchSeq() {
		clearNums();
		for (int i = getStart(); i < numElements(); i++) {
			int el = get(i);
			if (el >= 0 && el < digBase)
				incNum(el);
			else
				assert i == getStart();
		}
		assert numPieceMatches();
	}

	private boolean numPieceMatches() {
		if (!GenHasher.useToughAsserts())
			return true;
		if (testing == null)
			testing = new int[digBase];
		Arrays.fill(testing, 0);
		for (int i = getStart(); i < numElements(); i++) {
			int el = get(i);
			if (el >= 0 && el < digBase)
				testing[el]++;
			else
				assert i == getStart();
		}
		return Arrays.equals(testing, numPieces);
	}

	@Override
	protected boolean incr(int dir) {
		if (validLS())
			decNum(leastSig());
		if (super.incr(dir)) {
			incNum(leastSig());
			assert numPieceMatches();
			return true;
		} else {
			assert numPieceMatches();
			return false;
		}
	}

	@Override
	protected void trunc() {
		if (validLS())
			decNum(leastSig());
		super.trunc();
		assert numPieceMatches();
	}

	@Override
	protected void trunc(int startAt) {
		assert getStart() <= startAt;
		while (getStart() < startAt)
			trunc();
		assert numPieceMatches();
	}

	@Override
	protected void clear() {
		super.clear();
		clearNums();
		assert numPieceMatches();
	}

	@Override
	protected void addLS(int ls) {
		super.addLS(ls);
		incNum(ls);
		assert numPieceMatches();
	}

	/**
	 * @param digit
	 * @return
	 */
	public int numPieces(int digit) {
		return numPieces[digit];
	}

	@Override
	protected void set(int place, int val) {
		decNum(get(place));
		super.set(place, val);
		incNum(get(place));
		assert numPieceMatches();
	}
}
