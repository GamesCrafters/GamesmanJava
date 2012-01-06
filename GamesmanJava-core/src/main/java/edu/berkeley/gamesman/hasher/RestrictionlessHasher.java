package edu.berkeley.gamesman.hasher;

import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.hasher.invhasher.OptimizingInvariantHasher;

public class RestrictionlessHasher extends OptimizingInvariantHasher<GenState> {

	public RestrictionlessHasher(int numElements, int digitBase) {
		super(numElements, digitBase);
	}

	@Override
	protected int getInvariant(GenState state) {
		return 0;
	}

	@Override
	protected boolean valid(GenState state) {
		return true;
	}

	@Override
	protected int numInvariants(int startPoint) {
		return 1;
	}

	@Override
	protected GenState innerNewState() {
		return new GenState(this);
	}

}
