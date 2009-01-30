/**
 * 
 */
package edu.berkeley.gamesman.core;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Collection;
import edu.berkeley.gamesman.util.DependencyResolver;
import edu.berkeley.gamesman.util.OptionProcessor;
import edu.berkeley.gamesman.util.Util;

/**
 * Public interface that all Games must implement to be solvable
 * 
 * @author Steven Schlansker
 * @param <State> The object used to represent a Game State
 *
 */
public abstract class Game<State> implements Serializable {
	private static final long serialVersionUID = 6376065802238384739L;

	protected Hasher<State> hasher;
	
	protected final int gameWidth, gameHeight;
	
	static {
		OptionProcessor.nextGroup();
		OptionProcessor.acceptOption("gw", "width", true, "Width of the game board","<auto>");
		OptionProcessor.acceptOption("gh", "height", true, "Height of the game board","<auto>");
		OptionProcessor.nextGroup();
	}
	
	/**
	 * Initialize game width/height
	 */
	public Game(){
		String w = OptionProcessor.checkOption("width");
		String h = OptionProcessor.checkOption("height");
		if(w.equals("<auto>"))
			gameWidth = getDefaultBoardWidth();
		else
			gameWidth = Integer.parseInt(w);
		
		if(h.equals("<auto>"))
			gameHeight = getDefaultBoardHeight();
		else
			gameHeight = Integer.parseInt(h);
	}
	
	/**
	 * Generates all the valid starting positions
	 * @return a Collection of all valid starting positions
	 */
	public abstract Collection<State> startingPositions();
	
	/**
	 * Given a board state, generates all valid board states one move away from the given state
	 * @param pos The board state to start from
	 * @return All valid board states one move forward
	 */
	public abstract Collection<State> validMoves(State pos);
	
	/**
	 * Given a board state return its primitive "value".
	 * Usually this value includes WIN, LOSE, and perhaps TIE
	 * Return UNDECIDED if this is not a primitive state (shouldn't usually be called)
	 * @param pos The primitive State
	 * @return the Record representing the state
	 * @see edu.berkeley.gamesman.core.Record
	 */
	public abstract PrimitiveValue primitiveValue(State pos);
	
	/**
	 * Inform the Game of the hasher we're using
	 * @param h The Hasher to use
	 */
	private final void setHasher(Hasher<State> h){
		
		if(!DependencyResolver.isHasherAllowed(this.getClass(), h.getClass())){
			Util.fatalError("Hasher class "+h.getClass()+" inappropriate for game "+this.getClass());
		}
		
		hasher = h;
	}
	
	/**
	 * @param conf the Configuration that this game is played with
	 */
	public abstract void initialize(Configuration conf);
	
	/**
	 * Unhash a given hashed value and return the corresponding Board
	 * @param hash The hash given
	 * @return the State represented
	 */
	public abstract State hashToState(BigInteger hash);
	/**
	 * Hash a given state into a hashed value
	 * @param pos The State given
	 * @return The hash that represents that State
	 */
	public abstract BigInteger stateToHash(State pos);
	
	/**
	 * @return the last valid hash possible in the current configuration
	 */
	public abstract BigInteger lastHash();
	
	/**
	 * Produce a human-readable String representing the state
	 * @param pos the State given
	 * @return a String
	 */
	public abstract String stateToString(State pos);
	/**
	 * Given a String construct a State
	 * This is not necessarily compatible with stateToString
	 * @param pos The String given
	 * @return a State
	 */
	public abstract State stringToState(String pos);
	
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
	 * @return a String that uniquely describes the setup of this Game (including any variant information, game size, etc)
	 */
	public abstract String describe();
	
}
