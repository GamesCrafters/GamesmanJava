package edu.berkeley.gamesman.core;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.util.Pair;

/**
 * @author DNSpies
 */
public abstract class TieredIterGame extends TieredGame<ItergameState> implements Cloneable{

	/**
	 * @param conf The configuration object
	 */
	public TieredIterGame(Configuration conf) {
		super(conf);
	}

	@Override
	public PrimitiveValue primitiveValue(ItergameState pos){
		return unHashedClone(pos).primitiveValue();
	}

	/**
	 * Assumes the given state.
	 * 
	 * @param pos The position to assume
	 */
	public abstract void setState(ItergameState pos);

	private TieredIterGame unHashedClone(ItergameState pos) {
		TieredIterGame c = clone();
		c.setState(pos);
		return c;
	}
	
	/**
	 * @return The "primitive value" of the current position.
	 */
	public abstract PrimitiveValue primitiveValue();

	@Override
	public Collection<Pair<String, ItergameState>> validMoves(ItergameState pos) {
		return unHashedClone(pos).validMoves();
	}

	/**
	 * @return The states of all the possible moves from this position.
	 */
	public abstract Collection<Pair<String, ItergameState>> validMoves();

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
			setNumPieces(getTier()+1);
	}
	
	@Override
	public String stateToString(ItergameState pos){
		return unHashedClone(pos).stateToString();
	}
	
	/**
	 * @return A string representation of this position.
	 */
	public abstract String stateToString();
	
	@Override
	public ItergameState stringToState(String pos) {
		TieredIterGame c = clone();
		c.setFromString(pos);
		return c.getState();
	}
	
	/**
	 * @param pos A string representing this position
	 */
	public abstract void setFromString(String pos);

	/**
	 * Returns a state object for this position
	 * @return The state of this position
	 */
	public abstract ItergameState getState();

	@Override
	public abstract TieredIterGame clone();

	@Override
	public String displayState(ItergameState pos){
		return unHashedClone(pos).displayState();
	}
	
	/**
	 * @param tier The tier in question
	 * @return The number of hashes in the tier
	 */
	public long numHashesForTier(int tier){
		TieredIterGame s = clone();
		s.setNumPieces(tier);
		return s.numHashesForTier();
	}
	
	/**
	 * Sets this game to the given tier.
	 * @param tier The tier to set to.
	 */
	public abstract void setNumPieces(int tier);

	/**
	 * Pretty-print's the current position
	 * @return a string of the position
	 */
	public abstract String displayState();

	/**
	 * @return The number of hashes in this tier.
	 */
	public abstract long numHashesForTier();

	@Override
	public Collection<ItergameState> startingPositions() {
		ArrayList<ItergameState> positions = new ArrayList<ItergameState>();
		TieredIterGame pos;
		for (int i = 0; i < numStartingPositions(); i++) {
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
	
	public abstract int numberOfTiers();
}
