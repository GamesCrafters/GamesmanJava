package edu.berkeley.gamesman.solver;


import java.math.BigInteger;
import java.util.Collection;

import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.game.TieredGame;
import edu.berkeley.gamesman.util.Task;
import edu.berkeley.gamesman.util.Util;

/**
 * A generic solver that works top-down in the matter most games are played
 * Describes the game tree entirely.  Slow but reliable, also intuitive to understand
 * @author Steven Schlansker
 *
 */
public final class TierSolver extends Solver {


	private BigInteger calculated;
	
	@Override
	public void solve(Game<? extends Object, ?> igame) {
		Util.debug("Started the solver...");
		if(!(igame instanceof TieredGame))
			Util.fatalError("Attempted to use tiered solver on non-tiered game");
		TieredGame<Object,?> game = (TieredGame<Object,?>) igame;
		int numTier = game.numberOfTiers();
		
		Task t = Task.beginTask("Tier solving \""+igame.toString()+"\"");

		calculated = BigInteger.ZERO;
		
		t.setTotal(game.lastHashValueForTier(game.numberOfTiers()-1));
		calculated = BigInteger.ZERO;
		
		for(int i = numTier-1; i >= 0; i--){
			solveTier(game,i,t);
		}
		
		t.complete();
	}
	
	private void solveTier(TieredGame<Object,?> game, int tier, Task t){
		Util.debug("Beginning to solve tier "+tier);
		BigInteger start = game.hashOffsetForTier(tier).subtract(BigInteger.ONE);
		BigInteger current = start;
		BigInteger end = game.lastHashValueForTier(tier);
		while(current.compareTo(end) < 0){
			current = current.add(BigInteger.ONE);
			calculated = calculated.add(BigInteger.ONE);
			if(calculated.mod(BigInteger.valueOf(10000)).compareTo(BigInteger.ZERO) == 0)		
				t.setProgress(calculated);
			Object state = game.hashToState(current);
			//System.out.println(game.stateToString(state));
			Collection<?> children = game.validMoves(state);
			//System.out.println("State "+current+" has "+children.size()+" elts");
			if(children.size() == 0)
				db.setValue(current, game.positionValue(state));
		}
	}
	
}
