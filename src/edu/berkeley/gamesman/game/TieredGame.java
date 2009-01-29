package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.Arrays;

import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public abstract class TieredGame<State,Value> extends Game<State,Value> {

	protected BigInteger tierIndex[];
	
	public abstract int numberOfTiers();
	
	public BigInteger hashOffsetForTier(int tier){
		if(tierIndex == null) lastHashValueForTier(numberOfTiers()-1);
		return tierIndex[tier];
	}
	
	public BigInteger lastHashValueForTier(int tier){
		if(tierIndex == null){
			tierIndex = new BigInteger[numberOfTiers()];
			tierIndex[0] = BigInteger.ZERO;
			for(int i = 1; i < tierIndex.length; i++){
				tierIndex[i] = tierIndex[i-1].add(numHashesForTier(i-1)).add(BigInteger.ONE);
			}
			//Util.debug("Hash indices are "+Arrays.toString(tierIndex));
		}
		
		if(tier == tierIndex.length-1)
			return tierIndex[tier].add(numHashesForTier(tier-1));
		return tierIndex[tier+1].subtract(BigInteger.ONE);
	}
	public abstract BigInteger numHashesForTier(int tier);
	public abstract State gameStateForTierIndex(int tier, BigInteger index);
	public abstract Pair<Integer,BigInteger> tierIndexForState(State state);
	
	@Override
	public State hashToState(BigInteger hash) {
		for(int i = 0; i < numberOfTiers(); i++){
			if(lastHashValueForTier(i).compareTo(hash) >= 0)
				return gameStateForTierIndex(i,hash.subtract(hashOffsetForTier(i)));
		}
		
		BigInteger lastcheck = hash.subtract(tierIndex[tierIndex.length-1]);
		
		Util.debug("lastcheck = "+lastcheck);
		
		//if(lastcheck.compareTo(numHashesForTier(numberOfTiers()-1)) < 0)
				return gameStateForTierIndex(numberOfTiers()-1, lastcheck);
		
		//Util.fatalError("Hash outside of tiered values");
		//return null;
	}

	@Override
	public BigInteger stateToHash(State pos) {
		Pair<Integer,BigInteger> p = tierIndexForState(pos);
		return hashOffsetForTier(p.car).add(p.cdr);
	}
	
}
