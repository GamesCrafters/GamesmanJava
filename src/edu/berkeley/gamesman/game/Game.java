/**
 * 
 */
package edu.berkeley.gamesman.game;

import java.util.Collection;
import java.util.Iterator;

import edu.berkeley.gamesman.OptionProcessor;
import edu.berkeley.gamesman.hasher.Hasher;

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
	
	protected long gameWidth = 0, gameHeight = 0;
	
	static {
		OptionProcessor.nextGroup();
		OptionProcessor.acceptOption("gw", "width", true, "Width of the game board","7");
		OptionProcessor.acceptOption("gh", "height", true, "Height of the game board","6");
		OptionProcessor.nextGroup();
	}
	
	/**
	 * Initialize game width/height
	 */
	public Game(){
		gameWidth = Integer.parseInt(OptionProcessor.checkOption("width"));
		gameHeight = Integer.parseInt(OptionProcessor.checkOption("height"));
	}
	
	/**
	 * Generates an iterator over all the valid starting positions
	 * @return the iterator
	 */
	public abstract Collection<State> startingPositions();
	
	public abstract Iterator<State> validMoves(State pos);
	
	public abstract Value positionValue(State pos);
	
	public void setHasher(Hasher h){
		hasher = h;
	}
	
}
