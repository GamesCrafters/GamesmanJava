package edu.berkeley.gamesman.hasher;

import edu.berkeley.gamesman.hasher.fixed.FixedHasher;
import edu.berkeley.gamesman.hasher.fixed.FixedState;

/**
 * @author dnspies
 * 
 */
public final class DBHasher extends FixedHasher<FixedState> {
	/**
	 * @param numElements
	 * @param digitBase
	 * @param numPieces
	 */
	public DBHasher(int numElements, int digitBase, int[] numPieces) {
		super(numElements, digitBase, numPieces);
	}

	@Override
	protected int innerStep(FixedState state, int dir) {
		assert dir == 1 || dir == -1;
		int last = leastSig(state);
		boolean incred;
		do {
			incred = incr(state, dir);
		} while (incred && !validPref(state));
		int numTruncs = 0;
		if (!incred) {
			trunc(state);
			numTruncs = 1;
			if (isEmpty(state)) {
				addValid(state, dir == -1);
				return -1;
			}
			int ls = leastSig(state);
			while (dir == 1 && ls >= last || dir == -1 && ls <= last) {
				trunc(state);
				numTruncs++;
				if (isEmpty(state)) {
					for (int i = 0; i < numTruncs; i++)
						addValid(state, dir == -1);
					return -1;
				}
				last = ls;
				ls = leastSig(state);
			}
			do {
				incred = incr(state, dir);
				assert incred;
			} while (!validPref(state));
			addValid(state, dir == -1);
			last = leastSig(state);
			for (int i = 1; i < numTruncs; i++) {
				addLS(state, last);
				while (!validPref(state))
					incr(state, dir);
				last = leastSig(state);
			}
		}
		return getStart(state) + numTruncs + 1;
	}

	@Override
	protected boolean validPref(FixedState state) {
		return baseValidPref(state);
	}

	@Override
	protected FixedState innerNewState() {
		return new FixedState(this, numElements);
	}

	@Override
	protected int getInvariant(FixedState state) {
		return baseGetInvariant(state);
	}

	@Override
	protected int lastInvariant(FixedState state) {
		return baseLastInvariant(state);
	}

	@Override
	protected boolean valid(FixedState state) {
		return baseValid(state);
	}

	@Override
	protected int numInvariants(int startPoint) {
		return baseNumInvariants(startPoint);
	}
}
