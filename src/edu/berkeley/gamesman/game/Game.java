/**
 * 
 */
package edu.berkeley.gamesman.game;

import java.util.Iterator;

/**
 * Public interface that all Games must implement to be solvable
 * 
 * @author Steven Schlansker
 *
 */
public interface Game {

	/**
	 * Generates an iterator over all the valid starting positions
	 * @return the iterator
	 */
	public Iterator<GameState> startingPositions();
	
}
