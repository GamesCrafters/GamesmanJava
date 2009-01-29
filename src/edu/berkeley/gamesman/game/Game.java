/**
 * 
 */
package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;

import edu.berkeley.gamesman.hasher.Hasher;
import edu.berkeley.gamesman.util.DBEnum;
import edu.berkeley.gamesman.util.OptionProcessor;

/**
 * Public interface that all Games must implement to be solvable
 * 
 * @author Steven Schlansker
 * @param <State> The object used to represent a Game State
 * @param <Value> The object used to represent the value of a state
 *
 */
public abstract class Game<State,Value> {

	protected Hasher hasher;
	
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
	 * @see edu.berkeley.gamesman.game.Values
	 */
	public abstract Value positionValue(State pos);
	
	/**
	 * Inform the Game of the hasher we're using
	 * @param h The Hasher to use
	 */
	public void setHasher(Hasher h){
		hasher = h;
	}
	
	public abstract State hashToState(BigInteger hash);
	public abstract BigInteger stateToHash(State pos);
	
	public abstract String stateToString(State pos);
	public abstract BigInteger stringToState(String pos);
	
	public abstract int getDefaultBoardWidth();
	public abstract int getDefaultBoardHeight();
	
	/**
	 * Returns all possible primitive Values of a Position
	 * Note that if you are returning a primitive Enum class, you have to return
	 * an /instance/ of the Enum, not the class.  Therefore to indicate you want
	 * all values of enum Values to be valid, return any constant within Values.
	 * Ex: return edu.berkeley.gamesman.game.Values.INVALID;
	 * @return an instance of an Enum class with all representable states
	 * @see Values
	 */
	public abstract Enum possiblePositionValues();
	
}
