package edu.berkeley.gamesman.core;

import java.math.BigInteger;

import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * Superclass of Tiered games.
 * Each game state falls into a logical tier.  As an example, you can represent TicTacToe as a tiered game
 * with the tier being the number of pieces placed.
 * 
 * The important invariant is that any board's value must depend only on (a) primitives or (b) boards in a later tier.
 * This allows us to solve from the last tier up to the top at tier 0 (the starting state) in a very efficient manner
 * 
 * @author Steven Schlansker
 *
 * @param <State> The type that you use to represent your States
 */
public abstract class TieredGame<State> extends Game<State> {

	protected TieredHasher<State> myHasher;
	
	@Override
	public void initialize(Configuration conf){
		myHasher = (TieredHasher<State>)conf.getHasher();
	}
	
	@Override
	public State hashToState(BigInteger hash) {
		if(myHasher.cacheNumTiers == -1) myHasher.cacheNumTiers = myHasher.numberOfTiers();
		if(myHasher.tierIndex == null) myHasher.lastHashValueForTier(myHasher.cacheNumTiers-1);
		
		for(int i = 0; i < myHasher.cacheNumTiers; i++){
			if(myHasher.tierIndex[i].compareTo(hash) >= 0)
				if(i == 0)
					return myHasher.gameStateForTierIndex(i, hash);
				else
					return myHasher.gameStateForTierIndex(i,hash.subtract(myHasher.tierIndex[i-1]).subtract(BigInteger.ONE));
		}
		Util.fatalError("Hash outside of tiered values: "+hash);
		return null;
	}

	@Override
	public BigInteger stateToHash(State pos) {
		Pair<Integer,BigInteger> p = myHasher.tierIndexForState(pos);
		return myHasher.hashOffsetForTier(p.car).add(p.cdr);
	}
	
	/**
	 * @return the number of Tiers in this particular game
	 */
	public final int numberOfTiers(){
		return myHasher.numberOfTiers();
	}
	
	/**
	 * @param tier the Tier we're interested in
	 * @return the first hash value for that tier
	 */
	public final BigInteger hashOffsetForTier(int tier){
		return myHasher.hashOffsetForTier(tier);
	}
	
	/**
	 * @param tier the Tier we're interested in
	 * @return the last hash value that is still within that tier
	 */
	public final BigInteger lastHashValueForTier(int tier){
		return myHasher.lastHashValueForTier(tier);
	}
	

	@Override
	public final BigInteger lastHash() {
		return lastHashValueForTier(numberOfTiers()-1);
	}
	
}
