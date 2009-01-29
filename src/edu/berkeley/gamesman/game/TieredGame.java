package edu.berkeley.gamesman.game;

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
 * @param <Value> The possible values that a primitive State can have
 */
public abstract class TieredGame<State,Value> extends Game<State,Value> {

	protected BigInteger tierIndex[];
	
	/**
	 * Indicate the number of tiers that this game has
	 * (Note how this is distinct from the /last/ tier)
	 * @return Number of tiers.  Limited to java primitive int type for now
	 */
	public abstract int numberOfTiers();
	
	/**
	 * Return the first hashed value in a given tier
	 * @param tier The tier we're interested in
	 * @return The first hash value that will be in this tier
	 * @see #tierIndexForState
	 */
	public BigInteger hashOffsetForTier(int tier){
		if(tierIndex == null) lastHashValueForTier(numberOfTiers()-1);
		return tierIndex[tier];
	}
	
	/**
	 * Return the last hash value a tier represents
	 * @param tier The tier we're interested in
	 * @return The last hash that will be in the given tier
	 */
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
	/**
	 * Return the number of hashes in a tier
	 * @param tier The tier we're interested in
	 * @return Size of the tier
	 */
	public abstract BigInteger numHashesForTier(int tier);
	/**
	 * Convert a tier/index pair into a State
	 * Analogous to unhash for a Tiered game
	 * @param tier The tier we're in
	 * @param index The hash offset into that tier (relative to the start - 0 is the first board in the tier)
	 * @return The represented GameState
	 */
	public abstract State gameStateForTierIndex(int tier, BigInteger index);
	/**
	 * Convert a State into a tier/index pair
	 * Analogous to hash for a Tiered game
	 * @param state The game state we have
	 * @return a Pair with tier/offset enclosed
	 */
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
