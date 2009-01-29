package edu.berkeley.gamesman.core;

import java.math.BigInteger;

import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * Superclass of Tiered games.
 * Each game state falls into a logical tier.  As an example, you can represent TicTacToe as a tiered game
 * with the tier being the number of pieces placed.
 * 
 * The important invariant is that any board's value must depend only on (a) primitives or (b) boards in a higher tier.
 * This allows us to solve from the last tier up to the top at tier 0 (the starting state) in a very efficient manner
 * 
 * @author Steven Schlansker
 *
 * @param <State> The type that you use to represent your States
 */
public abstract class TieredGame<State> extends Game<State> {

	TieredHasher<State> th;
	
	@Override
	public void setHasher(Hasher<State> h){
		if(!(h instanceof TieredHasher)) Util.fatalError("Tiered game trying to work with non-tiered hasher: "+h.getClass());
		super.setHasher(h);
		th = (TieredHasher<State>)h;
	}
	
	@Override
	public State hashToState(BigInteger hash) {
		if(th.cacheNumTiers == -1) th.cacheNumTiers = th.numberOfTiers();
		if(th.tierIndex == null) th.lastHashValueForTier(th.cacheNumTiers-1);
		
		for(int i = 0; i < th.cacheNumTiers; i++){
			if(th.tierIndex[i].compareTo(hash) >= 0)
				if(i == 0)
					return th.gameStateForTierIndex(i, hash);
				else
					return th.gameStateForTierIndex(i,hash.subtract(th.tierIndex[i-1]).subtract(BigInteger.ONE));
		}
		Util.fatalError("Hash outside of tiered values: "+hash);
		return null;
	}

	@Override
	public BigInteger stateToHash(State pos) {
		Pair<Integer,BigInteger> p = th.tierIndexForState(pos);
		return th.hashOffsetForTier(p.car).add(p.cdr);
	}
	
	/**
	 * @return the number of Tiers in this particular game
	 */
	public final int numberOfTiers(){
		return th.numberOfTiers();
	}
	
	/**
	 * @param tier the Tier we're interested in
	 * @return the first hash value for that tier
	 */
	public final BigInteger hashOffsetForTier(int tier){
		return th.hashOffsetForTier(tier);
	}
	
	/**
	 * @param tier the Tier we're interested in
	 * @return the last hash value that is still within that tier
	 */
	public final BigInteger lastHashValueForTier(int tier){
		return th.lastHashValueForTier(tier);
	}
	
}
