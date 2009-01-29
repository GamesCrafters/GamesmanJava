package edu.berkeley.gamesman.solver;


import java.math.BigInteger;
import java.util.Collection;

import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.game.TieredGame;
import edu.berkeley.gamesman.util.ProgressMeter;
import edu.berkeley.gamesman.util.Util;

/**
 * A generic solver that works top-down in the matter most games are played
 * Describes the game tree entirely.  Slow but reliable, also intuitive to understand
 * @author Steven Schlansker
 *
 */
public final class TierSolver extends Solver {

	@Override
	public void solve(Game<?, ?> igame, ProgressMeter p) {
		Util.debug("Started the solver...");
		if(!(igame instanceof TieredGame))
			Util.fatalError("Attempted to use tiered solver on non-tiered game");
		TieredGame game = (TieredGame) igame;
		int maxTier = game.numberOfTiers();

		BigInteger calculated = BigInteger.ZERO;
		
		for(int i = maxTier-1; i >= 0; i--){
			Util.debug("Beginning to solve tier "+i);
			BigInteger start = game.hashOffsetForTier(i).subtract(BigInteger.ONE);
			BigInteger current = start;
			BigInteger end = game.lastHashValueForTier(i);
			while(current.compareTo(end) <= 0){
				current = current.add(BigInteger.ONE);
				calculated = calculated.add(BigInteger.ONE);
				if(calculated.mod(BigInteger.valueOf(10000)).compareTo(BigInteger.ZERO) == 0)		
					p.progress(calculated, end);
				Object state = game.hashToState(current);
				Collection<?> children = game.validMoves(state);
				if(children == null)
					continue;
				//System.out.println("State "+current+" has "+children.size()+" elts");
				if(children.size() == 0)
					db.setValue(current, game.positionValue(state));
			}
		}
	}
	
}
