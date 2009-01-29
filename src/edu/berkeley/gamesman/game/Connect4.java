/**
 * 
 */
package edu.berkeley.gamesman.game;

import java.util.Iterator;

import edu.berkeley.gamesman.core.SingletonIterator;

/**
 * @author Steven Schlansker
 *
 */
public final class Connect4 extends Game {

	class Connect4GameState implements GameState {

		@Override
		public Iterator<GameState> legalMoves() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	@Override
	public Iterator<GameState> startingPositions() {
		return new SingletonIterator<GameState>(new Connect4GameState());
	}

	

}
