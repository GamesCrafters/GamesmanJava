package edu.berkeley.gamesman.core;

import java.math.BigInteger;
import java.util.Arrays;

import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public abstract class TieredHasher<State> extends Hasher<State> {

	@Override
	public BigInteger hash(State board, int l) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigInteger maxHash(int boardlen) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State unhash(BigInteger hash, int l) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	/**
	 * Indicate the number of tiers that this game has
	 * (Note how this is distinct from the /last/ tier)
	 * @return Number of tiers.  Limited to java primitive int type for now
	 */
	public abstract int numberOfTiers();
	
	BigInteger tierIndex[];  // For tier i, tierIndex[i] is the /last/ hash value for that tier.

	int cacheNumTiers = -1;
	
	/**
	 * Return the first hashed value in a given tier
	 * @param tier The tier we're interested in
	 * @return The first hash value that will be in this tier
	 * @see #tierIndexForState
	 */
	public BigInteger hashOffsetForTier(int tier){
		if(tier == 0)
			return BigInteger.ZERO;
		if(tierIndex == null)
			lastHashValueForTier(numberOfTiers()-1);
		return tierIndex[tier-1].add(BigInteger.ONE);
	}
	
	/**
	 * Return the last hash value a tier represents
	 * @param tier The tier we're interested in
	 * @return The last hash that will be in the given tier
	 */
	public BigInteger lastHashValueForTier(int tier){
		if(tierIndex == null){
			tierIndex = new BigInteger[numberOfTiers()];
			for(int i = 0; i < tierIndex.length; i++){
				tierIndex[i] = hashOffsetForTier(i).add(numHashesForTier(i));
			}
			Util.debug("Created offset table: "+Arrays.toString(tierIndex));
		}
		return tierIndex[tier];
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
	

}