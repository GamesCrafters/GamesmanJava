package edu.berkeley.gamesman.game;

import java.util.Iterator;

/**
 * @author Steven Schlansker
 *
 */
public interface GameState {
	/**
	 * Generates an iterator that will return all valid child GameStates
	 * @return an iterator
	 */
	public Iterator<GameState> legalMoves();
}
