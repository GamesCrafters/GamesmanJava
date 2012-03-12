package edu.berkeley.gamesman.parallel.game.ninemensmorris;

import edu.berkeley.gamesman.hasher.counting.CountingState;
import edu.berkeley.gamesman.hasher.invhasher.OptimizingInvariantHasher;

public class NMMHasher extends OptimizingInvariantHasher<NMMState> {
	
	
	

	public NMMHasher(int[] digitBase) {
		super(digitBase);
		// TODO Auto-generated constructor stub
	}



	@Override
	protected NMMState innerNewState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected long getInvariant(NMMState state) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected boolean valid(NMMState state) {
		// TODO Auto-generated method stub
		return false;
	}
	
}
