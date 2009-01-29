/**
 * 
 */
package edu.berkeley.gamesman.game;

import java.util.Iterator;

import edu.berkeley.gamesman.OptionProcessor;

/**
 * Public interface that all Games must implement to be solvable
 * 
 * @author Steven Schlansker
 *
 */
public abstract class Game {

	
	static {
		OptionProcessor.acceptOption("w", "width", true, "Width of the game board");
		OptionProcessor.acceptOption("h", "height", true, "Height of the game board");
	}
	
	/**
	 * Generates an iterator over all the valid starting positions
	 * @return the iterator
	 */
	public abstract Iterator<GameState> startingPositions();
	
}
