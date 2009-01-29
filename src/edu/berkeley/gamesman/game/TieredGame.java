package edu.berkeley.gamesman.game;

import java.math.BigInteger;

import edu.berkeley.gamesman.Pair;

public interface TieredGame<State,Value> {

	public int lastTier();
	public BigInteger hashOffestForTier(int tier);
	public BigInteger lastHashValueForTier(int tier);
	public State gameStateForTierIndex(int tier, BigInteger index);
	public Pair<Integer,BigInteger> tierIndexForState(State state);
	
}
