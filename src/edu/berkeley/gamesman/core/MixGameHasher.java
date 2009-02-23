package edu.berkeley.gamesman.core;

import java.math.BigInteger;
import java.util.Collection;

/**
 * @author DNSpies
 * For a game that hashes its own positions.
 */
public abstract class MixGameHasher {
	protected final int gameWidth, gameHeight;
	protected final Configuration conf;
	
	/**
	 * Initialize game width/height
	 * NB: when this constructor is called, the Configuration
	 * is not required to have initialized the Hasher yet!
	 * @param conf configuration
	 */
	public MixGameHasher(Configuration conf){
		String w = conf.getProperty("gamesman.game.width","<auto>");
		String h = conf.getProperty("gamesman.game.height","<auto>");
		if(w.equals("<auto>"))
			gameWidth = getDefaultBoardWidth();
		else
			gameWidth = Integer.parseInt(w);
		
		if(h.equals("<auto>"))
			gameHeight = getDefaultBoardHeight();
		else
			gameHeight = Integer.parseInt(h);
		this.conf = conf;
	}
	
	/**
	 * Sets the board to its starting position.
	 */
	public abstract void setStartingPosition();

	/**
	 * Given a board state, generates all valid board states one move away from the given state
	 * @return A <move,state> pair for all valid board states one move forward
	 */
	public abstract Collection<BigInteger> validMoveHashes();
	
	/**
	 * Given a board state return its primitive "value".
	 * Usually this value includes WIN, LOSE, and perhaps TIE
	 * Return UNDECIDED if this is not a primitive state (shouldn't usually be called)
	 * @return the Record representing the state
	 * @see edu.berkeley.gamesman.core.Record
	 */
	public abstract PrimitiveValue primitiveValue();

	/**
	 * Hash a given state into a hashed value
	 * @return The hash that represents that State
	 */
	public abstract BigInteger getHash();
	
	/**
	 * @return the last valid hash possible in the current configuration
	 */
	public abstract BigInteger lastHash();
	
	/**
	 * @return Whether another position exists
	 */
	public abstract boolean hasNext();
	
	/**
	 * Cycle to the next position
	 */
	public abstract void nextPosition();
	
	/**
	 * "Pretty-print" a State for display to the user
	 * @return a pretty-printed string
	 */
	public abstract String displayState();
	
	/**
	 * @return the default board width for this game
	 */
	public abstract int getDefaultBoardWidth();
	/**
	 * @return the default board height for this game
	 */
	public abstract int getDefaultBoardHeight();
	
	/**
	 * @return the width of the board this game is played on
	 */
	public int getGameWidth(){
		return gameWidth;
	}
	
	/**
	 * @return the height of the board this game is played on
	 */
	public int getGameHeight(){
		return gameHeight;
	}
	
	/**
	 * Called to notify the Game that the Configuration now has
	 * a specified and initialized Hasher.
	 * Make sure to call your superclass's method!
	 */
	public void prepare(){}
	
	/**
	 * @return a String that uniquely identifies this Hasher (including valid pieces if appropriate, etc)
	 */
	public abstract String describe();
}
