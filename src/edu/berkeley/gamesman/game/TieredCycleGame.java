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
 */
public abstract class TieredCycleGame extends TieredGame<CycleState> implements Cloneable{

	/**
	 * @param conf The configuration object
	 */
	public TieredCycleGame(Configuration conf) {
		super(conf);
	}

	@Override
	public PrimitiveValue primitiveValue(CycleState pos){
		return unHashedClone(pos).primitiveValue();
	}

	/**
	 * Assumes the given state.
	 * 
	 * @param pos The position to assume
	 */
	public abstract void setState(CycleState pos);

	private TieredCycleGame unHashedClone(CycleState pos) {
		TieredCycleGame c = clone();
		c.setState(pos);
		return c;
	}
	
	/**
	 * @return The "primitive value" of the current position.
	 */
	public abstract PrimitiveValue primitiveValue();

	@Override
	public Collection<Pair<String, CycleState>> validMoves(CycleState pos) {
		return unHashedClone(pos).validMoves();
	}

	/**
	 * @return The states of all the possible moves from this position.
	 */
	public abstract Collection<Pair<String, CycleState>> validMoves();

	/**
	 * @return Whether there is another position.
	 */
	public boolean hasNext(){
		return hasNextHashInTier()||getTier()<numberOfTiers();
	}
	
	/**
	 * @return The tier of this position
	 */
	public abstract int getTier();

	/**
	 * Cycle to the next position.
	 */
	public void next(){
		if(hasNextHashInTier())
			nextHashInTier();
		else
			setTier(getTier()+1);
	}
	
	@Override
	public String stateToString(CycleState pos){
		return unHashedClone(pos).stateToString();
	}
	
	/**
	 * @return A string representation of this position.
	 */
	public abstract String stateToString();
	
	@Override
	public CycleState stringToState(String pos) {
		TieredCycleGame c = clone();
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
	public abstract CycleState getState();

	@Override
	public abstract TieredCycleGame clone();

	@Override
	public String displayState(CycleState pos){
		return unHashedClone(pos).displayState();
	}
	
	/**
	 * @param tier The tier in question
	 * @return The number of hashes in the tier
	 */
	public BigInteger numHashesForTier(int tier){
		TieredCycleGame s = clone();
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
	public Collection<CycleState> startingPositions() {
		ArrayList<CycleState> positions = new ArrayList<CycleState>(
				numStartingPositions());
		TieredCycleGame pos;
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

	/**
	 * @return Is there another hash for this tier?
	 */
	public abstract boolean hasNextHashInTier();

	/**
	 * Cycles to the next hash in this tier
	 */
	public abstract void nextHashInTier();
}
