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
	protected final int countTo;

	/**
	 * @param myHasher
	 */
	public CountingState(GenHasher<? extends CountingState> myHasher,
			int countTo) {
		super(myHasher);
		this.countTo = countTo;
		int base = 0;
		for (int i = 0; i < countTo; i++) {
			assert i == 0 || myHasher.baseFor(i) == base;
			base = Math.max(base, myHasher.baseFor(i));
		}
		this.numPieces = new int[base];
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
		for (int i = getStart(); i < countTo; i++) {
			int el = get(i);
			if (el >= 0 && el < digitBase())
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
			testing = new int[digitBase()];
		Arrays.fill(testing, 0);
		for (int i = getStart(); i < countTo; i++) {
			int el = get(i);
			if (el >= 0 && el < digitBase())
				testing[el]++;
			else
				assert i == getStart();
		}
		return Arrays.equals(testing, numPieces);
	}

	@Override
	protected boolean incr(int dir) {
		if (validLS())
			if (getStart() < countTo)
				decNum(leastSig());
		if (super.incr(dir)) {
			if (getStart() < countTo)
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
		if (validLS() && getStart() < countTo)
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
		if (getStart() < countTo)
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
		if (place < countTo)
			decNum(get(place));
		super.set(place, val);
		if (place < countTo)
			incNum(get(place));
		assert numPieceMatches();
	}

	protected final int digitBase() {
		return numPieces.length;
	}
}
