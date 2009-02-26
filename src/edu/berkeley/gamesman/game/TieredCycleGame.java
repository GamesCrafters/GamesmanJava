package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.util.Pair;

/**
 * @author DNSpies
 *
 * @param <State> A state usually containing its own hash
 */
public abstract class TieredCycleGame<State> extends TieredGame<State> implements Cloneable{

	/**
	 * @param conf The configuration object
	 */
	public TieredCycleGame(Configuration conf) {
		super(conf);
	}

	@Override
	public PrimitiveValue primitiveValue(State pos){
		return unHashedClone(pos).primitiveValue();
	}

	/**
	 * Assumes the given state.
	 * 
	 * @param pos The position to assume
	 */
	public abstract void setState(State pos);

	private TieredCycleGame<State> unHashedClone(State pos) {
		TieredCycleGame<State> c = clone();
		c.setState(pos);
		return c;
	}
	
	/**
	 * @return The "primitive value" of the current position.
	 */
	public abstract PrimitiveValue primitiveValue();

	@Override
	public Collection<Pair<String, State>> validMoves(State pos) {
		return unHashedClone(pos).validMoves();
	}

	/**
	 * @return The states of all the possible moves from this position.
	 */
	public abstract Collection<Pair<String, State>> validMoves();

	/**
	 * @return Whether there is another position.
	 */
	public abstract boolean hasNext();
	
	/**
	 * Cycle to the next position.
	 */
	public abstract void next();
	
	@Override
	public String stateToString(State pos){
		return unHashedClone(pos).stateToString();
	}
	
	/**
	 * @return A string representation of this position.
	 */
	public abstract String stateToString();
	
	@Override
	public State stringToState(String pos) {
		TieredCycleGame<State> c = clone();
		c.setToString(pos);
		return c.getState();
	}
	
	/**
	 * @param pos A string representing this position
	 */
	public abstract void setToString(String pos);

	/**
	 * Returns a state object for this position
	 * @return The state of this position
	 */
	public abstract State getState();

	@Override
	public abstract TieredCycleGame<State> clone();

	@Override
	public String displayState(State pos){
		return unHashedClone(pos).displayState();
	}
	
	/**
	 * @param tier The tier in question
	 * @return The number of hashes in the tier
	 */
	public BigInteger numHashesForTier(int tier){
		TieredCycleGame<State> s = clone();
		s.setTier(tier);
		return s.numHashesForTier();
	}
	
	/**
	 * Sets this game to the given tier.
	 * @param tier The tier to set to.
	 */
	public abstract void setTier(int tier);

	/**
	 * Pretty-print's the current position
	 * @return a string of the position
	 */
	public abstract String displayState();

	/**
	 * @return The number of hashes in this tier.
	 */
	public abstract BigInteger numHashesForTier();

	@Override
	public Collection<State> startingPositions() {
		ArrayList<State> positions = new ArrayList<State>(
				numStartingPositions());
		TieredCycleGame<State> pos;
		for (int i = 0; i < positions.size(); i++) {
			pos = clone();
			pos.setStartingPosition(i);
			positions.add(pos.getState());
		}
		return positions;
	}
	
	/**
	 * Sets this board to a starting position.
	 * @param n The number position to set to
	 */
	public abstract void setStartingPosition(int n);

	/**
	 * @return The number of possible starting positions.
	 */
	public abstract int numStartingPositions();
}
