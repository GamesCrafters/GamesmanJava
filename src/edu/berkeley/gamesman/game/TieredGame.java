package edu.berkeley.gamesman.game;

import java.math.BigInteger;

import edu.berkeley.gamesman.util.Pair;

public abstract class TieredGame<State,Value> extends Game<State,Value> {

	public abstract int numberOfTiers();
	public abstract BigInteger hashOffestForTier(int tier);
	public abstract BigInteger lastHashValueForTier(int tier);
	public abstract State gameStateForTierIndex(int tier, BigInteger index);
	public abstract Pair<Integer,BigInteger> tierIndexForState(State state);
	
}
