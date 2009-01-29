package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.Pair;

public interface TieredGame<State,Value> {

	public Number lastTier();
	public Number lastHashValueForTier(Number tier);
	public State gameStateForTierIndex(Number tier, Number index);
	public Pair<Number,Number> tierIndexForState(State state);
	
}
