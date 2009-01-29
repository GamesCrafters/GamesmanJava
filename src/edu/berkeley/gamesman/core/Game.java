/**
 * 
 */
package edu.berkeley.gamesman.core;

import java.math.BigInteger;
import java.util.Collection;
import edu.berkeley.gamesman.database.DBValue;
import edu.berkeley.gamesman.util.DependencyResolver;
import edu.berkeley.gamesman.util.OptionProcessor;
import edu.berkeley.gamesman.util.Util;

/**
 * Public interface that all Games must implement to be solvable
 * 
 * @author Steven Schlansker
 * @param <State> The object used to represent a Game State
 * @param <Value> The object used to represent the value of a state
 *
 */
public abstract class Game<State,Value extends DBValue> {

	protected Hasher<State> hasher;
	
	protected int gameWidth = 0, gameHeight = 0;
	
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
	 * @param The board state to start from
	 * @return All valid board states one move forward
	 */
	public abstract Collection<State> validMoves(State pos);
	
	/**
	 * Given a board state return its primitive "value".
	 * Usually this value includes WIN, LOSE, and perhaps TIE -
	 * the canonical representation is in the game.Values enum
	 * Return UNDECIDED if this is not a primitive state (shouldn't usually be called)
	 * @param pos The primitive State
	 * @return the Value representing the state
	 * @see edu.berkeley.gamesman.core.Values
	 */
	public abstract Value primitiveValue(State pos);
	
	/**
	 * Inform the Game of the hasher we're using
	 * @param h The Hasher to use
	 */
	public void setHasher(Hasher<State> h){
		
		if(!DependencyResolver.isHasherAllowed(this.getClass(), h.getClass())){
			Util.fatalError("Hasher class "+h.getClass()+" inappropriate for game "+this.getClass());
		}
		
		hasher = h;
	}
	
	public abstract State hashToState(BigInteger hash);
	public abstract BigInteger stateToHash(State pos);
	
	public abstract String stateToString(State pos);
	public abstract State stringToState(String pos);
	
	public abstract int getDefaultBoardWidth();
	public abstract int getDefaultBoardHeight();
	
	public int getGameWidth(){
		return gameWidth;
	}
	
	public int getGameHeight(){
		return gameHeight;
	}
	
	public abstract DBValue getDBValueExample();
	
}
